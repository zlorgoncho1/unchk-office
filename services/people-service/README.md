# people-service

Service propriétaire des **entités canoniques** de la plateforme UNCHK Office :
**Étudiant** (INE, identité, formation, promotion, années, diplômes) et
**Personnel / Formateur**. Référencées par UUID partout ailleurs.

- **Port** : `8082` (host) — écoute `8080` dans le réseau Docker `unchk-net`.
- **Base PostgreSQL** : `people` (`jdbc:postgresql://postgres:5432/people`).
- **Topics produits** : `people.students`, `people.staff`.
- **Topic consommé** : `identity.users` (read-model local `identity_user_ro`).
- **Paquet de base** : `sn.unchk.office.people`.

## Principes d'architecture respectés

- **Spring Boot 3.3.4 / Java 21**, **Spring Web MVC** (servlet, pas réactif).
- **Inter-services = 100% Kafka** : aucun appel REST vers un autre service. Les besoins
  d'autres agrégats (comptes utilisateurs) sont satisfaits par une **projection CQRS locale**
  alimentée en consommant `identity.users`.
- **CQRS / Outbox-like** : chaque écriture persiste dans la base `people` **et** émet
  l'événement correspondant sur le topic concerné.
- **Clés primaires UUID** (anti-énumération / anti-IDOR).
- **Flyway** (`db/migration/V1__init.sql`) ; Hibernate `ddl-auto=validate`.
- **Sécurité** : JWT validé via JWKS d'identity-service (librairie `common`), **ABAC OPA au
  niveau objet** sur la fiche étudiant, Bean Validation sur tous les DTO.

## Endpoints REST

Routés par le gateway via `/api/people/**` et `/api/etudiants/**`.

### Étudiants — `/api/people/students`

| Méthode | Chemin | Rôle attendu (RBAC gateway) | Notes |
|---|---|---|---|
| `GET` | `/api/people/students` | personnel | Liste paginée des étudiants actifs |
| `GET` | `/api/people/students/{id}` | personnel | **ABAC OPA** (anti-IDOR) ; 404 si refusé |
| `POST` | `/api/people/students` | admin / administratif | Crée + émet `people.students` (`Created`) |
| `PUT` | `/api/people/students/{id}` | admin / administratif | **ABAC OPA** ; émet `Updated` |
| `DELETE` | `/api/people/students/{id}` | admin / administratif | Suppression logique ; émet `Deleted` (tombstone) |

### Fiche personnelle de l'étudiant — `/api/etudiants/me`

| Méthode | Chemin | Rôle | Notes |
|---|---|---|---|
| `GET` | `/api/etudiants/me` | etudiant | **Anti-IDOR fort** : `me` résolu côté serveur via le claim `sub` → `students.user_ref`. Aucune fiche d'un autre étudiant n'est atteignable. |

### Personnel / Formateurs — `/api/people/staff`

| Méthode | Chemin | Rôle | Notes |
|---|---|---|---|
| `GET` | `/api/people/staff` | personnel | Liste paginée |
| `GET` | `/api/people/staff/{id}` | personnel | Consultation |
| `POST` | `/api/people/staff` | admin / administratif | Crée + émet `people.staff` (`Created`) |
| `PUT` | `/api/people/staff/{id}` | admin / administratif | Émet `Updated` |
| `DELETE` | `/api/people/staff/{id}` | admin / administratif | Suppression logique ; émet `Deleted` |

## Événements Kafka émis

Enveloppe `DomainEvent<T>` (librairie `common`) en valeur ; métadonnées dans les **en-têtes**
Kafka (`eventId`, `eventType`, `eventVersion`, `aggregateType`, `aggregateId`, `occurredAt`,
`traceId`, `producer`). **Clé de partition** = UUID de l'agrégat.

| Topic | Politique | Clé | Payloads |
|---|---|---|---|
| `people.students` | `compact` | `studentId` | `StudentPayload` (`Created`/`Updated`), `TombstonePayload` (`Deleted`) |
| `people.staff` | `compact` | `staffId` | `StaffPayload` (`Created`/`Updated`), `TombstonePayload` (`Deleted`) |

Les topics sont créés explicitement (`KafkaTopicsConfig`, `NewTopic`) car l'auto-création est
désactivée côté broker.

## Read-models locaux (projections CQRS)

| Table `_ro` | Source Kafka | Usage |
|---|---|---|
| `identity_user_ro` | `identity.users` | Relier un étudiant à son compte (`user_ref`) pour l'accès `me` ; afficher l'auteur. |

Idempotence des consommateurs via la table `processed_events(event_id)` : un événement rejoué
(reprise, retry, replay d'un topic compacté) est ignoré.

## Contrôle d'accès au niveau objet (anti-IDOR)

- L'annotation `@VerifieAccesObjet(type = "etudiant", ...)` (librairie `common`) déclenche
  l'aspect `ResourceAccessGuard` qui interroge OPA **avant** de renvoyer/modifier la fiche.
- `FournisseurAttributsEtudiant` charge l'étudiant et expose à OPA son `ownerId`
  (compte `user_ref` de l'étudiant) et sa `visibility` (rôles de gestion). OPA tranche en
  *deny-by-default*.
- Sur refus en **lecture**, le service renvoie **404** (anti-énumération), indistinct d'une
  ressource inexistante.

## Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8082` | Port HTTP |
| `DB_URL` | `jdbc:postgresql://postgres:5432/people` | Datasource |
| `DB_USER` / `DB_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants base |
| `KAFKA_BOOTSTRAP` | `kafka:19092` | Broker Kafka (listener interne) |
| `JWT_JWKS_URI` | `http://identity-service:8080/.well-known/jwks.json` | Clés publiques JWKS |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `unchk-office` | Validation du JWT |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC objet) |

## Build (via Docker, image Maven — pas de Maven local)

Contexte de build = **racine du dépôt** :

```bash
docker build -f services/people-service/Dockerfile -t people-service .
```

Le `Dockerfile` est multi-étapes : il installe d'abord `libs/common`, puis package le service,
et produit une image d'exécution basée sur `eclipse-temurin:21-jre`.

## Tests

```bash
mvn -f services/people-service/pom.xml test
```

- `StudentServiceTest` : logique métier (création + événement, conflit d'INE, fiche `me`,
  suppression logique).
- `PeopleEventPublisherTest` : topic, clé de partition et en-têtes de l'enveloppe Kafka.
- `IdentityUserConsumerTest` : upsert du read-model + **idempotence** + tombstone.
- `StudentControllerTest` : validation 400, 404 anti-énumération, création 201.
