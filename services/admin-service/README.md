# admin-service — Administration (budget)

Microservice **event-driven** de la plateforme UNCHK Office (Université Cheikh Hamidou Kane).
Il prend en charge la **gestion budgétaire** : projet de budget, **note d'orientation** budgétaire
et **budget réalisé** (prévu vs réalisé par poste), avec **exports PDF / Excel** des états.

- **Port** : `8087` (overridable par `SERVER_PORT`).
- **Base PostgreSQL** : `admin` (`jdbc:postgresql://postgres:5432/admin`).
- **Topic produit** : `admin.budget`.
- **Topic consommé** : `people.staff` (read-model local `people_staff_ro`).
- **Paquet de base** : `sn.unchk.office.admin`.
- **Stack** : Spring Boot 3.3.4 / Java 21, Spring **WEB MVC** (servlet, pas réactif),
  Spring Data JPA, Flyway, Spring Kafka. Dépend de la librairie partagée
  `sn.unchk.office:common:0.1.0-SNAPSHOT`.

## Principes d'architecture respectés

- **Inter-services = 100% Kafka** : aucun appel REST entre microservices. Le service publie ses
  événements budgétaires sur `admin.budget` et maintient son read-model `people_staff_ro` en
  consommant `people.staff` (projection CQRS, idempotence via `processed_events`).
- **Sécurité fédérée** : validation des JWT RS256 via le JWKS d'identity-service (apportée par
  `common`). Toute requête (hors sonde de santé) exige un jeton valide.
- **Anti-IDOR (ABAC objet)** : les endpoints sensibles portent `@VerifieAccesObjet` (libs/common) ;
  le `FournisseurAttributsAdmin` charge le **propriétaire** (`ownerId`) et la **visibilité** réels
  d'un budget en base, et OPA tranche (`/v1/data/unchk/authz/allow`). Un budget n'est accessible
  par son UUID que si OPA l'autorise (propriétaire, rôle visible, ou admin).
- **Clés primaires UUID** partout (anti-énumération).
- **Bean Validation** sur tous les DTO d'entrée ; mapping explicite DTO ↔ entité (anti
  sur-affectation). Les champs système (`id`, `ownerId`, `createdBy`, totaux) ne sont jamais liés
  depuis le corps client : `ownerId` / `createdBy` proviennent du JWT.
- **Migrations Flyway** depuis le DDL des docs (`db/migration/V1__init.sql`) ;
  `hibernate ddl-auto=validate`.

## Modèle de données (base `admin`)

| Table | Rôle |
|---|---|
| `budgets` | Budget d'un exercice : statut, note d'orientation, totaux prévu/réalisé, propriétaire (ABAC). |
| `budget_lines` | Lignes budgétaires : prévu vs réalisé par poste (dépense/recette). |
| `mails` | Courrier arrivé/départ (structure créée par la migration). |
| `admin_communiques` | Notes de service & circulaires. |
| `communique_targets` | Ciblage par rôle des communiqués. |
| `people_staff_ro` | **Read-model** (projection Kafka de `people.staff`) — lecture seule. |
| `processed_events` | Idempotence de la consommation Kafka (déduplication sur `eventId`). |

> Le périmètre fonctionnel codé en API porte sur la **gestion budgétaire** (cœur du service) ;
> les tables courrier/communiqués sont créées par la migration pour la complétude du schéma `admin`.

## API REST (sous `/api/admin`)

| Méthode & chemin | Description | Anti-IDOR |
|---|---|---|
| `POST /api/admin/budgets` | Créer un projet de budget | — |
| `GET /api/admin/budgets?annee=2026` | Lister les budgets (filtre exercice optionnel) | — |
| `GET /api/admin/budgets/{id}` | Consulter un budget (entête + lignes + écarts) | `read` |
| `PUT /api/admin/budgets/{id}` | Mettre à jour (libellé, note d'orientation, devise) | `update` |
| `PATCH /api/admin/budgets/{id}/statut` | Faire évoluer le statut | `update` |
| `POST /api/admin/budgets/{id}/lignes` | Ajouter une ligne (poste prévu) | `update` |
| `PATCH /api/admin/budgets/{id}/lignes/{ligneId}/realisation` | Saisir le budget réalisé d'une ligne | `update` |
| `GET /api/admin/budgets/{id}/export/pdf` | Export PDF de l'état budgétaire | `read` |
| `GET /api/admin/budgets/{id}/export/excel` | Export Excel (xlsx) de l'état budgétaire | `read` |

Les totaux prévu/réalisé et les écarts sont **recalculés** à partir des lignes à chaque
modification, puis l'état est publié sur `admin.budget` (CQRS).

## Événements Kafka

- **Produits** sur `admin.budget` (clé = UUID du budget, topic compacté) :
  `BudgetCree`, `BudgetMisAJour`, `BudgetStatutModifie`, `BudgetRealiseMisAJour`.
  L'enveloppe `DomainEvent` (valeur JSON) et les en-têtes (`eventId`, `eventType`, `aggregateType`,
  `aggregateId`, `occurredAt`, `traceId`, `producer`) suivent `docs/architecture.md`.
- **Consommés** depuis `people.staff` : upsert idempotent de `people_staff_ro`.

## Configuration (variables d'environnement clés)

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8087` | Port d'écoute du service. |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | base `admin` | Source de données PostgreSQL. |
| `KAFKA_BOOTSTRAP_INTERNAL` | `kafka:19092` | Broker Kafka (listener interne). |
| `JWT_JWKS_URI` / `JWT_ISSUER` / `JWT_AUDIENCE` | identity-service | Validation des jetons. |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC anti-IDOR). |
| `CONFIG_SERVER_URL` | `http://config-server:8888` | Config Server (import **optionnel**). |

## Build (via Docker, sans Maven local)

Le `Dockerfile` est multi-étapes ; le **contexte de build est la racine du dépôt**. Il installe
d'abord `libs/common`, puis construit le jar du service :

```bash
docker build -f services/admin-service/Dockerfile -t unchk/admin-service .
```

## Tests

Tests JUnit 5 (commentaires en français) couvrant : calcul des écarts (`BudgetMapperTest`),
unicité + recalcul des totaux + émission d'événement (`BudgetServiceTest`), idempotence du
consommateur (`PeopleStaffConsumerTest`) et enrichissement ABAC anti-IDOR
(`FournisseurAttributsAdminTest`).
