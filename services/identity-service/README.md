# identity-service — UNCHK Office

Fournisseur d'identité **fédéré maison** (sans Keycloak) de la plateforme UNCHK Office.
Source de vérité des comptes utilisateurs et des rôles. Émet des **JWT RS256**, expose un
endpoint **JWKS**, gère les **refresh tokens** (révocation / rotation) et journalise l'audit
d'authentification.

- **Port** : `8081`
- **Base PostgreSQL** : `identity`
- **Topic Kafka produit** : `identity.users`
- **Paquet de base** : `sn.unchk.office.identity`

## Rôle dans l'architecture

`identity-service` est l'**autorité d'émission** des jetons. À la connexion, il vérifie les
identifiants (BCrypt) et délivre un access token RS256 + un refresh token. Tous les autres
composants (gateway, services métier) valident les jetons via le **JWKS** exposé ici. Aucune
clé privée n'est détenue ailleurs. Le topic `identity.users` propage rôles et statut
(`ACTIVE` / `SUSPENDED` / `DISABLED`) pour la révocation rapide côté gateway/OPA.

Communication inter-services **100 % Kafka** : ce service ne fait aucun appel REST vers les
autres. Il maintient un read-model local (`read_person`) en consommant `people.students` et
`people.staff` (projection CQRS) pour valider le `person_ref` d'un compte.

## Endpoints REST

Tous routés par le gateway via le préfixe `/api/identity/**`.

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| `POST` | `/api/identity/auth/login` | public | Connexion : renvoie access + refresh tokens |
| `POST` | `/api/identity/auth/refresh` | public | Rafraîchit la paire de jetons (rotation) |
| `POST` | `/api/identity/auth/logout` | public | Révoque les refresh tokens de l'utilisateur |
| `GET` | `/api/identity/auth/.well-known/jwks.json` | public | Clés **publiques** (JWKS) via le gateway |
| `GET` | `/.well-known/jwks.json` | public | Clés **publiques** (JWKS) — accès direct gateway/services |
| `GET` | `/api/identity/users` | admin | Liste des comptes |
| `GET` | `/api/identity/users/{id}` | admin + ABAC | Détail d'un compte (anti-IDOR via OPA) |
| `POST` | `/api/identity/users` | admin | Création d'un compte (émet `identity.users`) |
| `PUT` | `/api/identity/users/{id}` | admin + ABAC | Mise à jour (nom, statut, rôles) |
| `PUT` | `/api/identity/users/{id}/password` | admin + ABAC | Réinitialisation du mot de passe |
| `DELETE` | `/api/identity/users/{id}` | admin + ABAC | Suppression logique (émet `identity.users`) |

## Sécurité

- **JWT RS256** signés par la clé active (table `signing_keys`), avec rotation : les anciennes
  clés publiques restent dans le JWKS le temps que les jetons en cours expirent.
- **JWKS** : seules les clés publiques sont exposées ; la clé privée n'est ni exposée ni publiée
  sur Kafka.
- **Mots de passe** : hachés en **BCrypt**, jamais stockés ni journalisés en clair.
- **Refresh tokens** : seul le **hash SHA-256** est persisté ; révocation explicite et rotation
  anti-rejeu à chaque rafraîchissement.
- **Anti-bruteforce** : verrouillage du compte après N échecs (`LOGIN_MAX_FAILED`, défaut 5).
- **Audit OWASP A09** : table `auth_audit` (`LOGIN_OK`, `LOGIN_FAIL`, `LOCK`, `LOGOUT`, ...).
- **ABAC anti-IDOR** : la consultation/modification d'un compte par UUID passe par OPA
  (`@VerifieAccesObjet`, librairie `common`) ; le propriétaire (ou l'admin) seul y accède.
- **Validation** : Bean Validation sur tous les DTO d'entrée ; DTO dédiés (anti sur-affectation).

## Modèle de données (base `identity`)

Migrations Flyway dans `src/main/resources/db/migration/` :

- `V1__init.sql` — DDL des docs : `users`, `user_roles`, `signing_keys`, `refresh_tokens`,
  `auth_audit` (+ type `role_code`). Clés primaires UUID.
- `V2__read_models.sql` — `processed_events` (idempotence consommateurs) et `read_person`
  (projection locale des personnes canoniques).

Hibernate est en `ddl-auto=validate` : le schéma est entièrement géré par Flyway.

## Événements Kafka

- **Produit** `identity.users` (clé = `userId`) à chaque création / mise à jour / suppression de
  compte ou changement de rôle. Enveloppe `DomainEvent` (commune) + en-têtes
  (`eventId`, `eventType`, `aggregateType`, `aggregateId`, `traceId`, `producer`...).
  La charge utile transporte l'état du compte (identité, rôles, statut) — **jamais** de hash.
- **Consomme** `people.students` et `people.staff` pour alimenter `read_person` (idempotence sur
  `eventId`).

## Configuration (variables d'environnement principales)

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8081` | Port d'écoute |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `postgres` / `5432` / `identity` | Datasource |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `unchk` / `unchk_dev_pwd` | Identifiants DB |
| `KAFKA_BOOTSTRAP` | `kafka:19092` | Broker Kafka (listener interne) |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `unchk-office` | `iss` / `aud` des jetons émis |
| `JWT_ACCESS_TTL_MIN` | `30` | Durée de vie access token (minutes) |
| `JWT_REFRESH_TTL_DAYS` | `7` | Durée de vie refresh token (jours) |
| `LOGIN_MAX_FAILED` | `5` | Seuil de verrouillage anti-bruteforce |
| `OPA_URL` | `http://opa:8181` | PDP OPA (ABAC objet) |
| `CONFIG_IMPORT` | `optional:configserver:...` | Import optionnel du Config Server |

## Build et exécution (via Docker, pas de Maven local)

Le `Dockerfile` est multi-étapes ; le **contexte de build est la racine du dépôt** (accès au
`pom.xml` parent et à `libs/common`).

```bash
# Depuis la racine du dépôt
docker compose build identity-service
docker compose up -d identity-service
```

## Tests

Tests unitaires JUnit 5 (sans infrastructure) couvrant : les rôles, le comportement de
verrouillage du compte, la conversion PEM des clés RSA, l'émission/vérification des JWT RS256,
le flux d'authentification (connexion, échec + verrouillage, rafraîchissement) et la gestion
des comptes (création, conflit d'email, non-fuite des secrets).
