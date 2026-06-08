# document-service

Service de **gestion documentaire** de la plateforme UNCHK Office (Université Cheikh Hamidou Kane).

- Port HTTP : `8083`
- Base PostgreSQL dédiée : `document`
- Topic Kafka produit : `document.documents` (politique `delete`, clé = `documentId`)
- Stockage binaire : **MinIO** (buckets `documents` et `courriers`)

## Périmètre

Gestion documentaire mutualisée (Communication ET Administration s'appuient dessus) :
courrier arrivé/départ, notes de service, notes administratives, circulaires, rapports.

- Les **métadonnées** vivent dans PostgreSQL (`documents`, `document_visibility`, `document_shares`).
- Le **binaire** vit dans MinIO ; la base ne garde que le couple `(bucket, object_key)` (unique).
- La **visibilité par rôle** (`document_visibility`) alimente le `visibility[]` exposé à OPA
  pour le contrôle d'accès **au niveau objet (anti-IDOR)** au téléchargement et à la consultation.

## Architecture (rappels non négociables)

- **Spring Boot 3.3.4 / Java 21**, Spring Web **MVC** (servlet), paquet `sn.unchk.office.document`.
- **Communication inter-services = 100% Kafka.** Aucun appel REST entre services. Le service
  maintient un **read-model local** (`identity_user_ro`) en consommant `identity.users` (CQRS).
- **PostgreSQL** via spring-data-jpa, clés primaires **UUID** (anti-énumération),
  `hibernate ddl-auto=validate`, schéma géré par **Flyway** (`db/migration/V1__init.sql`).
- **Transactional Outbox** (`outbox`) : atomicité « écriture base + publication Kafka » ;
  un relais planifié publie les événements sur `document.documents` (en-têtes = enveloppe
  `DomainEvent` : `eventId`, `eventType`, `eventVersion`, `aggregateType`, `aggregateId`, ...).
- **Idempotence consommateur** : table `processed_events` (déduplication sur `eventId`).
- **Sécurité** : JWT RS256 validé via le **JWKS** d'identity-service (librairie `common`) ;
  **OPA au niveau objet** (anti-IDOR) sur les endpoints sensibles via l'annotation
  `@VerifieAccesObjet` + `FournisseurAttributsDocument` (charge `ownerId` + `visibility[]`).

## Endpoints (`/api/documents`)

| Méthode | Chemin | Description | Contrôle |
|---|---|---|---|
| `POST` | `/api/documents` | Dépôt d'un document (multipart : `metadata` JSON + `file`) | RBAC route |
| `GET` | `/api/documents` | Liste paginée (`?category=...`) | RBAC route |
| `GET` | `/api/documents/recherche` | Recherche par titre (`?q=...`) | RBAC route |
| `GET` | `/api/documents/{id}` | Métadonnées d'un document | **OPA objet (read)** |
| `GET` | `/api/documents/{id}/telechargement` | URL présignée MinIO temporaire | **OPA objet (read)** |
| `PATCH` | `/api/documents/{id}` | Mise à jour métadonnées / visibilité | **OPA objet (update)** |
| `POST` | `/api/documents/{id}/partages` | Partage nominatif | **OPA objet (update)** |
| `DELETE` | `/api/documents/{id}` | Suppression logique | **OPA objet (delete)** |

> Sur refus OPA en **lecture**, la réponse reste indistincte d'une **404** (anti-énumération).

### Anti-IDOR (anti-énumération)

Le RBAC (rôle × route) au gateway ne suffit pas : un utilisateur pourrait deviner l'UUID d'un
document. Avant tout accès à un document désigné par son UUID, la garde OPA charge `ownerId`
et `visibility[]` depuis la base locale et demande la décision au PDP (deny-by-default). Un
document inconnu ou non autorisé en lecture renvoie **404** (et non 403) pour ne pas confirmer
l'existence de l'UUID.

## Durcissement upload (OWASP)

- Taille maximale par fichier (par défaut 25 Mo) + limite multipart Spring.
- **Liste blanche** de types MIME (`document.upload.types-mime-autorises`).
- Clé objet MinIO non devinable (`<uuid>/<nom-nettoyé>`), checksum SHA-256 stocké.

## Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `postgres` / `5432` / `document` | Base PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants base |
| `KAFKA_BOOTSTRAP` | `kafka:19092` | Broker Kafka (listener interne) |
| `JWT_JWKS_URI` / `JWT_ISSUER` / `JWT_AUDIENCE` | — | Validation des JWT (JWKS) |
| `OPA_URL` | `http://opa:8181` | Serveur OPA (PDP) |
| `MINIO_ENDPOINT` | `http://minio:9000` | API MinIO |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants MinIO |
| `CONFIG_SERVER_URI` | `http://config-server:8888` | Config Server (import optionnel) |

## Build (via Docker, contexte = racine du dépôt)

```bash
docker build -f services/document-service/Dockerfile -t unchk/document-service .
```

Le `Dockerfile` est multi-étapes : il installe d'abord `libs/common`, puis package le service
(jar Spring Boot exécutable) et l'exécute sur une image JRE 21.

## Tests

Tests unitaires JUnit 5 (Mockito) — commentaires en français :

- `ProjectionUtilisateurServiceTest` : idempotence + tombstone de la projection CQRS.
- `FournisseurAttributsDocumentTest` : enrichissement ABAC (anti-IDOR) pour OPA.
- `DocumentServiceTest` : validation d'upload, dépôt MinIO, écriture Outbox.
- `CategorieDocumentTest` : conversion énuméré ↔ code base.
