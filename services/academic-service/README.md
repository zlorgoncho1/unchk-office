# academic-service

Service **académique** de la plateforme UNCHK Office (Université Cheikh Hamidou Kane).
Microservice Spring Boot 3.3.4 / Java 21, **MVC servlet** (pas réactif), architecture
**event-driven** (Kafka KRaft), une base PostgreSQL dédiée (`academic`).

- **Port interne** : `8085` (réseau Docker `unchk-net`)
- **Base** : `jdbc:postgresql://postgres:5432/academic`
- **Topic produit** : `academic.formations` (compacté, clé = `formationId`)
- **Topic consommé** : `people.staff` (projection locale des formateurs)
- **Préfixe REST** : `/api/academic` (routé par l'API Gateway)

## Périmètre fonctionnel

Module **Formations** (cf. `docs/specifications.md`) :

1. **Formations** : intitulé, code, type, niveau, période (début/fin), source de financement,
   effectifs **formés par genre** (`trained_male` / `trained_female`).
2. **Emplois du temps** : créneaux récurrents (jour de la semaine) ou ponctuels (date), horaires,
   salle / lien visio, intervenant.
3. **Affectation des formateurs** : un formateur (référence `people.staff.id`) affecté à une
   formation pour un module (matière).
4. **Statistiques + export PDF / Excel** des effectifs par genre.

## Communication inter-services (100 % Kafka, zéro REST)

Le service ne fait **jamais** d'appel REST vers un autre microservice. Les noms des formateurs
sont résolus localement via une **projection CQRS** :

```
topic people.staff ──► StaffProjectionConsumer ──► table academic_formateur_ro (read-model)
```

- Le consommateur **déduplique** sur `eventId` (table `processed_events`) — indispensable car les
  topics compactés peuvent être rejoués depuis l'offset 0.
- Toute évolution d'une formation est **publiée** sur `academic.formations` (enveloppe
  `DomainEvent` de `libs/common`) pour alimenter les projections des autres services
  (people, insertion, communication, admin).

## Sécurité

- **JWT / JWKS** : validation des jetons RS256 émis par identity-service, via la configuration
  du serveur de ressources de `libs/common` (`unchk.security.jwt.*`).
- **ABAC anti-IDOR (OPA)** : les endpoints qui ciblent un objet par UUID sont annotés
  `@VerifieAccesObjet` (aspect `ResourceAccessGuard` de `libs/common`). Le service fournit les
  attributs de la ressource (`FournisseurAttributsFormation` : `ownerId` = responsable/créateur,
  `visibility` = rôles) ; OPA tranche (`/v1/data/unchk/authz/allow`). En cas de refus en lecture,
  une erreur générique est renvoyée (anti-énumération).
- **Bean Validation** sur tous les DTO d'entrée.
- **UUID** comme clés primaires (anti-énumération).

## Modèle de données (Flyway `V1__init.sql`)

| Table | Rôle |
|---|---|
| `formations` | Agrégat racine (source de vérité locale). |
| `formation_formateurs` | Affectation formateur × module (PK composite). |
| `schedule_slots` | Créneaux d'emploi du temps. |
| `academic_formateur_ro` | **Read-model** des formateurs (projection `people.staff`). |
| `processed_events` | Idempotence des consommateurs Kafka. |

`hibernate.ddl-auto=validate` : Hibernate ne crée jamais le schéma, Flyway en est seul responsable.

## Endpoints principaux (`/api/academic`)

| Méthode & chemin | Description | Anti-IDOR |
|---|---|---|
| `GET /formations` | Liste des formations (filtre `?niveau=`) | RBAC route |
| `GET /formations/{id}` | Détail d'une formation | ABAC objet (read) |
| `POST /formations` | Créer une formation | RBAC route |
| `PUT /formations/{id}` | Modifier une formation | ABAC objet (update) |
| `DELETE /formations/{id}` | Supprimer (logique) une formation | ABAC objet (delete) |
| `GET /formateurs` | Formateurs connus (projection locale) | RBAC route |
| `GET /formations/{id}/formateurs` | Formateurs affectés (noms résolus localement) | ABAC objet (read) |
| `POST /formations/{id}/formateurs` | Affecter un formateur (module) | ABAC objet (update) |
| `DELETE /formations/{id}/formateurs/{ref}?module=` | Retirer une affectation | ABAC objet (update) |
| `GET /emplois-du-temps/{formationId}` | Emploi du temps (accès étudiant) | ABAC objet (read) |
| `GET /formations/{id}/creneaux` | Créneaux (vue gestion) | ABAC objet (read) |
| `POST /formations/{id}/creneaux` | Ajouter un créneau | ABAC objet (update) |
| `DELETE /formations/{id}/creneaux/{creneauId}` | Supprimer un créneau | ABAC objet (update) |
| `GET /statistiques/formations.pdf` | Export PDF des effectifs par genre | RBAC route |
| `GET /statistiques/formations.xlsx` | Export Excel des effectifs par genre | RBAC route |

## Construction (Docker, sans Maven local)

Le `Dockerfile` est **multi-étapes**, contexte de build = **racine** du dépôt :

```bash
# depuis la racine du dépôt
docker build -f services/academic-service/Dockerfile -t unchk/academic-service .
```

Étapes : compilation de `libs/common`, puis du module `services/academic-service`, puis image
JRE légère exécutant le jar (`EXPOSE 8085`).

## Tests

Tests JUnit 5 (commentaires en français) :

- `FormationServiceTest` : création, conflit de code, période incohérente, 404, suppression.
- `StaffProjectionConsumerTest` : projection `people.staff` (upsert, idempotence, suppression).
- `CreneauServiceTest` : règles de cohérence des créneaux (récurrence exclusive, horaires).
- `FournisseurAttributsFormationTest` : attributs ABAC (propriétaire, visibilité, anti-énumération).
- `FormationPayloadTest` : transfert d'état vers Kafka (énumérations en chaînes neutres).

## Variables d'environnement utiles

| Variable | Défaut | Rôle |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/academic` | URL de la base |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants base |
| `KAFKA_BOOTSTRAP` | `kafka:19092` | Broker Kafka (listener interne) |
| `JWT_JWKS_URI` | `http://identity-service:8080/.well-known/jwks.json` | Clés publiques JWKS |
| `JWT_ISSUER_URI` | `http://identity-service:8080` | Émetteur attendu |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC objet) |
| `CONFIG_SERVER_URI` | `http://config-server:8888` | Config Server (import optionnel) |
