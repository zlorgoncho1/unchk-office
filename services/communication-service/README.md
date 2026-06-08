# communication-service

Microservice **Communication** de la plateforme UNCHK Office (Université Cheikh Hamidou Kane).

Gère les **comptes rendus** (réunions, séminaires, webinaires, Conseil d'Université), les
**réunions** et les **notifications temps réel** poussées via **WebSocket**. Architecture
event-driven : communication inter-services **100 % Apache Kafka** (aucun appel REST entre
services), read-models locaux (CQRS), sécurité JWT/JWKS + OPA (ABAC anti-IDOR).

- **Port** : `8084`
- **Base PostgreSQL** : `communication`
- **Paquet de base** : `sn.unchk.office.communication`
- **Endpoints REST** : sous `/api/communication`
- **WebSocket** : `/ws/notifications` (STOMP)

## Topics Kafka

| Topic | Sens | Rôle |
|---|---|---|
| `communication.comptesrendus` | **produit** + consommé (self) | Comptes rendus ; la publication déclenche les notifications. |
| `communication.reunions` | **produit** + consommé (self) | Réunions ; la planification déclenche les convocations. |
| `notifications` | **produit** + consommé (self) | Push temps réel (clé = `recipientId`, 6 partitions). |
| `identity.users` | consommé | Projection `identity_user_ro` (destinataires par rôle). |
| `people.staff` | consommé | Projection `people_staff_ro` (nom auteur / organisateur). |
| `document.documents` | consommé | Déclenche les notifications de **circulaire** publiée. |

L'enveloppe d'événement (`eventId`, `eventType`, `eventVersion`, `aggregateType`,
`aggregateId`, `occurredAt`, `traceId`, `producer`) est portée par les **en-têtes Kafka** ;
la valeur ne porte que l'état de l'agrégat (JSON), conformément à `docs/architecture.md`.

## Flux de notification (compte rendu → WebSocket)

1. `POST /api/communication/comptes-rendus` → persistance d'un brouillon + événement
   `CompteRenduRedige`.
2. `PATCH /api/communication/comptes-rendus/{id}/publish` → événement `CompteRenduPublie`
   (réservé au propriétaire, ABAC `update`).
3. Le service **consomme son propre topic** : il résout les destinataires depuis ses
   **read-models locaux** (`identity_user_ro` filtré par la `visibility` du compte rendu),
   **sans aucun appel REST**, et met en file une notification par destinataire sur
   `notifications`.
4. Le consommateur de `notifications` persiste la notification (badge + historique) puis
   **pousse** un frame STOMP vers la session WebSocket du destinataire (liée à son
   `subject.id` → pas d'IDOR temps réel cross-utilisateur).

Même mécanique pour les **circulaires** (`document.documents`, catégorie `circulaire`) et les
**convocations de réunion** (`communication.reunions`, `ReunionPlanifiee` → participants).

## Sécurité (anti-IDOR)

- **JWT RS256** validé via le **JWKS** d'identity-service (serveur de ressources de
  `libs/common`).
- **ABAC objet** via OPA (`@VerifieAccesObjet`) sur la lecture/publication d'un compte rendu :
  le `ownerId` (créateur) et la `visibility` (rôles) **réels** sont chargés en base par
  `FournisseurAttributsCommunication` et soumis à OPA. Refus en lecture → **404** (anti-énumération).
- **Notifications** bornées au destinataire courant (`subject.id` résolu **côté serveur**,
  jamais via un identifiant fourni par le client).
- **Bean Validation** sur tous les DTO d'entrée ; champs système jamais liés depuis le corps.

## Modèle de données (base `communication`)

Tables maîtresses : `reunions`, `reunion_participants`, `comptes_rendus`,
`compte_rendu_visibility`, `notifications`.
Read-models (`_ro`, lecture seule, alimentés par Kafka) : `identity_user_ro`,
`people_staff_ro`.
Support : `processed_events` (idempotence consommateur), `outbox` (Transactional Outbox).
Clés primaires **UUID** (anti-IDOR). Schéma versionné par **Flyway**
(`db/migration/V1__init.sql`), `hibernate ddl-auto=validate`.

## Garanties techniques

- **Transactional Outbox** (`outbox` + `RelaisOutbox`) : atomicité écriture base + publication
  Kafka. Le relais émet périodiquement les messages non publiés avec les en-têtes d'enveloppe.
- **Idempotence** consommateur via `processed_events(event_id)` (déduplication sur `eventId`).
- **Ordre** garanti par la clé de partition (UUID de l'agrégat / du destinataire).

## Construction (Docker, contexte = racine du dépôt)

Aucune compilation Maven locale n'est requise (build via image Maven). Depuis la racine :

```bash
docker build -f services/communication-service/Dockerfile -t communication-service .
```

Le `Dockerfile` est multi-étapes : il installe d'abord `libs/common`, puis empaquette le
service, et exécute le jar sur une image JRE 21 (port `8084`).

## Variables d'environnement principales

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8084` | Port HTTP du service. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/communication` | Base PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `communication` | Identifiants base. |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:19092` | Bus Kafka (réseau Docker). |
| `JWT_JWKS_URI` | `http://identity-service:8080/.well-known/jwks.json` | Clés publiques JWKS. |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `http://identity-service:8080` / `unchk-office` | Validation JWT. |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC objet). |

## Tests

Tests unitaires JUnit 5 / Mockito (commentaires en français) couvrant : rédaction/publication
de compte rendu, résolution des destinataires (read-model local), attributs ABAC anti-IDOR,
persistance + push de notification, extraction d'enveloppe Kafka.
