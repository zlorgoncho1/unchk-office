# UNCHK Office

Plateforme web de gestion administrative et pédagogique de l'**Université Cheikh Hamidou Kane**
(Master Ingénierie Logicielle P8 — Technologie d'Application Web).

Architecture **monorepo / microservices event-driven** : front Angular, API Gateway (tier Middle),
microservices Spring Boot communiquant **exclusivement via Kafka**, identité fédérée maison (JWT/JWKS,
sans Keycloak), autorisation externalisée **OPA**, stockage objet **MinIO**, base **PostgreSQL**.

## Architecture

```
Browser ─REST/WS─► api-gateway ─REST─► [identity | people | document | communication | academic | insertion | admin]
                       │  (valide JWT via JWKS, demande l'autorisation à OPA)
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
| Frontend | Angular + Angular Material (servi par nginx) |
| Middle | Spring Cloud Gateway + OPA (authz) |
| Backend | Spring Boot 3 / Java 21, microservices |
| Messaging | Apache Kafka (KRaft natif, sans ZooKeeper) |
| Identité | identity-service maison (JWT RS256 + JWKS) |
| Données | PostgreSQL 16 (une base par service) |
| Objets | MinIO (S3-compatible) |
| Config | Spring Cloud Config Server |
| Orchestration | Docker Compose |

## Prérequis

- Docker + Docker Compose
- JDK 21 et Maven (build local), Node LTS + Angular CLI (front)

## Démarrage (infrastructure)

```bash
cp .env.example .env
docker compose up -d kafka postgres minio opa kafka-ui
```

| Service | URL locale |
|---|---|
| PostgreSQL | localhost:5432 |
| Kafka (broker) | localhost:9092 |
| Kafka UI | http://localhost:8085 |
| MinIO API / Console | http://localhost:9000 / http://localhost:9001 |
| OPA | http://localhost:8181 |

## Sécurité (transverse, par défaut)

Durcissement **OWASP Top 10 + IDOR** mutualisé, jamais réimplémenté par service :
- **Gateway (middleware)** : validation JWT (issuer/audience/expiry via JWKS), autorisation OPA, en-têtes
  de sécurité (CSP, HSTS, X-Frame-Options, X-Content-Type-Options…), CORS en liste blanche, rate-limiting,
  limite de taille de requête, identifiant de corrélation.
- **libs/common (partagé)** : garde d'**autorisation au niveau objet** (anti-IDOR) via OPA
  (`sujet × action × ressource × attributs`), validation des DTO, journalisation d'audit, gestion d'erreurs
  qui ne fuite aucune interne, IDs **opaques (UUID)** anti-énumération.
- **OPA** : politiques Rego **RBAC** (rôle × route) **+ ABAC** (accès au niveau objet), *deny-by-default*.

## Structure du dépôt

```
apps/        frontend (Angular) + api-gateway
services/    microservices métier (un module Maven chacun)
libs/        common (JWT, OPA, Kafka, export PDF/Excel)
platform/    opa (politiques Rego), db (init), config (Spring Cloud Config)
docs/        spécifications, schéma BD, documentation technique
```
