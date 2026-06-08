# UNCHK Office

Plateforme web de gestion administrative et pédagogique de l'**Université Cheikh Hamidou Kane**
(Master Ingénierie Logicielle P8 — Technologie d'Application Web).

Architecture **monorepo / microservices event-driven** : front Angular, API Gateway (tier Middle),
7 microservices Spring Boot communiquant **exclusivement via Kafka**, identité fédérée maison
(JWT/JWKS, sans Keycloak), autorisation externalisée **OPA**, stockage objet **MinIO**, base **PostgreSQL**.

## Architecture

```
Browser ─REST/WS─► api-gateway ─REST─► [identity | people | document | communication | academic | insertion | admin]
                       │  (valide le JWT via JWKS, demande l'autorisation à OPA)
                       └─WebSocket push notifications
                                            ▲
   Inter-services ◄────────── Kafka (KRaft) ──────────►   (SEUL canal entre microservices, zéro REST)
                                            │
   PostgreSQL (1 base / service)     MinIO (objets)     OPA (PDP Rego)     Config Server (Spring Cloud)
```

**Principe clé** : aucun appel REST entre microservices. Chaque service maintient un **read-model
local** (projection CQRS) alimenté en consommant les topics Kafka des autres services.

## Stack

| Couche | Techno |
|---|---|
| Frontend | Angular 19 + Angular Material 3 (charte UNCHK, Iconify/Solar) |
| Middle | Spring Cloud Gateway (réactif) + OPA (autorisation) |
| Backend | Spring Boot 3.3 / Java 21, 7 microservices |
| Messaging | Apache Kafka (KRaft natif, sans ZooKeeper) |
| Identité | identity-service maison (JWT RS256 + JWKS) |
| Données | PostgreSQL 16 (une base par service, migrations Flyway) |
| Objets | MinIO (S3-compatible) |
| Config | Spring Cloud Config Server |
| Orchestration | Docker Compose |
| Tests | JUnit (backend), Playwright (E2E) |

## Prérequis

- **Docker** + Docker Compose (le backend Java se compile dans des conteneurs Maven — **pas de JDK/Maven requis en local**).
- **Node 20.19+ / 22.12+** et npm (frontend Angular + Playwright).

## Démarrage rapide (pile complète)

```bash
cp .env.example .env

# Construit (Maven dans Docker + build Angular) puis lance les 16 conteneurs
# (7 microservices + gateway + config + frontend + infra).
# Premier lancement : long (images + dépendances Maven/npm, cache ensuite).
docker compose up -d --build

# Vérifier que tout est démarré / "healthy"
docker compose ps
```

> **Une seule commande suffit** : `docker compose up` démarre **toute l'application** (frontend + backend).

Le **frontend** est servi sur **http://localhost:4200** (nginx) et le **gateway** expose l'API sur `http://localhost:8080`. Ouvrez http://localhost:4200 et connectez-vous. Compte de démonstration (seedé par Flyway) :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin@unchk.sn` | `Admin123!` | admin |

Vérification rapide en ligne de commande :

```bash
# 1. Connexion -> jeton JWT
curl -s -X POST localhost:8080/api/identity/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@unchk.sn","motDePasse":"Admin123!"}'

# 2. Appel protégé (remplacer <JWT> par l'accessToken obtenu)
curl -s -H "Authorization: Bearer <JWT>" localhost:8080/api/people/students
```

### Frontend (mode développement, optionnel)

> Le frontend de production est déjà servi par `docker compose` sur http://localhost:4200.
> Ce mode n'est utile que pour le rechargement à chaud pendant le développement
> (arrêter d'abord le conteneur : `docker compose stop frontend`).

```bash
cd apps/frontend
npm install
npx ng serve        # http://localhost:4200 (appelle le gateway sur :8080)
```

### Ports

| Service | URL locale |
|---|---|
| **Frontend (nginx)** | http://localhost:4200 |
| **API Gateway** | http://localhost:8080 |
| identity / people / document / communication / academic / insertion / admin | 8081 … 8087 (port hôte de debug → 8080 interne) |
| Config Server | http://localhost:8888 |
| Kafka (broker) | localhost:9092 |
| Kafka UI | http://localhost:8090 |
| MinIO API / Console | http://localhost:9000 / http://localhost:9001 |
| OPA | http://localhost:8181 |
| PostgreSQL | localhost:5432 |
| JWKS (clés publiques) | http://localhost:8081/.well-known/jwks.json |

## Tests

```bash
# Tests unitaires backend (JUnit, via Docker Maven — cache .m2 partagé)
docker run --rm -v "$PWD":/work -v unchk-m2:/root/.m2 -w /work \
  maven:3.9-eclipse-temurin-21 mvn -B test

# Tests E2E (Playwright, pile démarrée requise)
cd apps/frontend
npx playwright install chromium
npx playwright test
```

## Sécurité (transverse, par défaut)

Durcissement **OWASP Top 10 + IDOR** mutualisé, jamais réimplémenté par service :
- **Gateway** : validation JWT (issuer/audience/expiration via JWKS), autorisation **OPA** (rôle × route),
  en-têtes de sécurité (CSP, HSTS, X-Frame-Options…), CORS en liste blanche, rate-limiting, identifiant de corrélation.
- **libs/common (partagé)** : garde d'**autorisation au niveau objet** (anti-IDOR) via OPA
  (`sujet × action × ressource × attributs`), validation des DTO, audit, erreurs sans fuite, IDs **UUID** anti-énumération.
- **OPA** : politiques Rego **RBAC** (rôle × route) **+ ABAC** (accès au niveau objet), *deny-by-default*.

## Structure du dépôt

```
apps/        frontend (Angular + e2e Playwright) + api-gateway
services/    7 microservices métier (un module Maven chacun, migrations Flyway)
libs/        common (sécurité JWT, client OPA, Kafka, export PDF/Excel, audit)
platform/    opa (politiques Rego), db (init), config-server (Spring Cloud Config)
brand/       logos + tokens de la charte graphique
docs/        spécifications, schéma BD, sécurité, architecture, rapport technique, journal de bord
```

## Documentation

| Document | Contenu |
|---|---|
| [`docs/rapport-technique.md`](docs/rapport-technique.md) | **Rapport technique résumé** (synthèse pour évaluation) |
| [`docs/architecture.md`](docs/architecture.md) | Architecture event-driven, topics Kafka, CQRS, sécurité |
| [`docs/database.md`](docs/database.md) | Schéma de base de données par service |
| [`docs/security.md`](docs/security.md) | Durcissement OWASP Top 10 + anti-IDOR |
| [`docs/specifications.md`](docs/specifications.md) | Spécifications fonctionnelles (rôles × modules) |
| [`docs/brand.md`](docs/brand.md) | Charte graphique UNCHK |
| [`docs/journal-de-bord.md`](docs/journal-de-bord.md) | Problèmes rencontrés et solutions (20 entrées) |
