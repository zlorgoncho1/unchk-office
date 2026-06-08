# insertion-service — Appui à l'insertion

Microservice de l'**Université Cheikh Hamidou Kane** (UNCHK Office). Il assure l'appui à
l'insertion professionnelle : suivi des étudiants (registre de contact), bilans de stages,
base de données des partenaires et statistiques d'insertion (auto-emploi vs emploi salarié).

- **Port** : `8086`
- **Base PostgreSQL** : `insertion`
- **Topic Kafka émis** : `insertion.events` (clé de partition = `studentId`)
- **Topics consommés** (read-models locaux) : `people.students`, `academic.formations`
- **Paquet de base** : `sn.unchk.office.insertion`

## Architecture (rappels non négociables)

- Spring Boot 3.3.4 / Java 21, **Spring MVC (servlet, pas réactif)**.
- Communication inter-services **100% Apache Kafka (KRaft)** : **aucun appel REST** entre
  services. Les données possédées par d'autres services sont répliquées localement en
  **read-models** (projections CQRS) alimentés par Kafka.
- PostgreSQL via Spring Data JPA, **clés primaires UUID** (anti-IDOR), schéma géré par
  **Flyway** (`db/migration/V1__init.sql`), `hibernate.ddl-auto=validate`.
- Sécurité : **JWT validé via JWKS** d'identity-service (librairie `common`), **OPA au
  niveau objet** (ABAC anti-IDOR) sur les endpoints sensibles, **Bean Validation** sur les DTO.

## Modèle de données (`insertion`)

| Table | Rôle |
|---|---|
| `partners` | Partenaires / structures d'accueil (entreprise, administration, ONG...). |
| `internships` | Stages et bilans de stages (réf. étudiant, partenaire, tuteur, rapport). |
| `contact_log` | Registre de contact (suivi du devenir des diplômés). |
| `insertion_outcomes` | Situations d'insertion (auto-emploi vs salarié) — support des stats. |
| `people_student_ro` | **Read-model** projeté depuis `people.students` (genre, formation). |
| `academic_formation_ro` | **Read-model** projeté depuis `academic.formations` (libellé). |
| `processed_events` | Journal d'idempotence Kafka (dédoublonnage sur `eventId`). |

## API REST (sous `/api/insertion`)

| Méthode + chemin | Description | ABAC objet |
|---|---|---|
| `GET /partenaires` | Liste des partenaires actifs | — |
| `GET /partenaires/{id}` | Consultation d'un partenaire | oui (`partenaire`) |
| `POST /partenaires` | Création d'un partenaire | — |
| `PUT /partenaires/{id}` | Mise à jour | oui |
| `DELETE /partenaires/{id}` | Suppression logique | oui |
| `GET /stages` (`?etudiant=`) | Liste des stages (filtrable par étudiant) | — |
| `GET /stages/{id}` | Consultation d'un stage | oui (`stage`) |
| `POST /stages` | Création d'un stage | — |
| `PUT /stages/{id}` | Mise à jour / clôture du bilan | oui |
| `DELETE /stages/{id}` | Suppression logique | oui |
| `GET /contacts/etudiant/{studentRef}` | Historique de contact d'un étudiant | oui (`insertion`) |
| `POST /contacts` | Enregistrer un contact de suivi | — |
| `GET /situations/etudiant/{studentRef}` | Situations d'insertion d'un étudiant | oui (`insertion`) |
| `GET /situations/{id}` | Consultation d'une situation | oui |
| `POST /situations` | Déclarer une situation d'insertion | — |
| `PUT /situations/{id}` | Mise à jour d'une situation | oui |
| `GET /statistiques` | Statistiques d'insertion (JSON) | — |
| `GET /statistiques/export/pdf` | Export PDF des statistiques | — |
| `GET /statistiques/export/excel` | Export Excel (xlsx) des statistiques | — |

Le **RBAC grossier** (rôle × route) est appliqué au gateway via OPA : seuls `admin` et
`appui-insertion` accèdent à `/api/insertion/**` (cf. `platform/opa/policies/data.json`).
L'**ABAC fin** (anti-IDOR au niveau objet) est appliqué dans ce service via l'annotation
`@VerifieAccesObjet` de la librairie `common` : un étudiant ne peut consulter que SON propre
suivi (le propriétaire ABAC d'un stage / d'une situation est l'étudiant concerné).

## Événements émis (`insertion.events`)

Enveloppe `DomainEvent` (librairie `common`), valeur JSON, clé = UUID de l'étudiant.

`PartenaireCree` · `PartenaireModifie` · `PartenaireSupprime` · `StageCree` · `StageModifie`
· `StageValide` · `ContactEnregistre` · `InsertionDeclaree` · `InsertionModifiee`.

Ces événements sont consommés par admin-service (statistiques), communication-service (suivi)
et people-service (cf. `docs/architecture.md`).

## Read-models (consommateurs Kafka)

- `people.students` → `people_student_ro` (genre + formation, pour les stats par genre).
- `academic.formations` → `academic_formation_ro` (libellé, pour étiqueter les stats).

Les consommateurs sont **idempotents** (dédoublonnage sur `eventId` via `processed_events`)
et gèrent les **tombstones** (`Deleted` / `deletedAt`) en retirant l'entrée projetée.

## Configuration

Valeurs par défaut adaptées au réseau Docker (`docker-compose`) ; surchargées par variables
d'environnement et, optionnellement, par le Config Server.

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8086` | Port HTTP |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `postgres` / `5432` / `insertion` | Datasource |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants DB |
| `KAFKA_BOOTSTRAP` | `kafka:19092` | Broker Kafka (KRaft) |
| `JWT_JWKS_URI` / `JWT_ISSUER` / `JWT_AUDIENCE` | identity-service | Validation JWT |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC anti-IDOR) |
| `CONFIG_IMPORT` | `optional:configserver:http://config-server:8888` | Config Server (optionnel) |

> Note : le Config Server fournit déjà `insertion-service.yml` avec `server.port: 8086`. La
> route du gateway (`apps/api-gateway`) pointe vers le nom d'hôte `insertion-service` ; le port
> applicatif est `8086` conformément à `docs/architecture.md §9.3`.

## Construction et exécution

Build via Docker (contexte = **racine du dépôt**, Dockerfile multi-étapes, aucun Maven local
requis) :

```bash
docker build -f services/insertion-service/Dockerfile -t unchk/insertion-service .
```

## Tests

Tests unitaires JUnit 5 (commentaires en français) :

- `StatistiquesServiceTest` — agrégation des statistiques (global + par formation).
- `ConsommateurProjectionsTest` — projection, idempotence, suppression logique.
- `PartenaireServiceTest` — persistance + publication d'événement Kafka.
- `ValidationDtoTest` — contraintes Bean Validation des DTO.
- `FournisseurAttributsInsertionTest` — attributs ABAC anti-IDOR.
