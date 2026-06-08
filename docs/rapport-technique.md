# Rapport technique résumé — UNCHK Office

Master Ingénierie Logicielle P8 — Technologie d'Application Web · Université Cheikh Hamidou Kane.

Ce document synthétise la conception et la réalisation. Les détails sont dans
[`architecture.md`](architecture.md), [`database.md`](database.md), [`security.md`](security.md),
[`specifications.md`](specifications.md) et [`journal-de-bord.md`](journal-de-bord.md).

## 1. Objectif

Concevoir une application web **modulaire et évolutive** pour la gestion administrative et pédagogique
de l'université, avec une **architecture en trois couches** (Front Angular → API REST → Back Spring Boot)
et un espace d'accueil par **rôle** (admin, administratif, enseignant, appui-insertion, étudiant).

## 2. Architecture générale

Architecture **monorepo, microservices event-driven** :

```
Angular (4200) ─REST/WS─► API Gateway (8080) ─REST─► 7 microservices Spring Boot
                              │ valide JWT (JWKS) + autorise via OPA
                              └─ WebSocket (notifications)
        Inter-services : Apache Kafka (KRaft) UNIQUEMENT — aucun appel REST entre services
        Données : PostgreSQL (1 base/service) · Objets : MinIO · Config : Spring Cloud Config
```

- **Tier Front** : Angular 19 + Material 3, charte graphique UNCHK (palette dérivée du logo, icônes Solar),
  app shell (barre latérale + topbar + rail droit) et un tableau de bord par rôle.
- **Tier Middle** : **Spring Cloud Gateway** (réactif), point d'entrée unique. Il valide le JWT via le
  JWKS d'identity, puis délègue l'**autorisation à OPA** (RBAC rôle × route), applique les en-têtes de
  sécurité, le CORS, le rate-limiting et la corrélation.
- **Tier Back** : 7 microservices (identity, people, document, communication, academic, insertion, admin),
  un **bounded context** chacun, une **base PostgreSQL** dédiée et des migrations **Flyway**.

## 3. Communication inter-services (event-driven / CQRS)

Choix structurant : les microservices **ne s'appellent jamais en REST**. Toute communication passe par
**Apache Kafka en mode KRaft** (sans ZooKeeper). Chaque service :
- **publie** des événements de domaine sur ses topics (ex. `people.students`, `academic.formations`) ;
- **consomme** les topics dont il a besoin pour maintenir des **read-models locaux** (projections CQRS,
  tables `*_ro`), avec idempotence (table `processed_events`).

Exemple : `academic-service` connaît les formateurs via une projection alimentée par `people.staff`, sans
jamais interroger `people-service`. Le flux de **notifications temps réel** suit le même principe :
publication d'un compte rendu → topic `notifications` → push **WebSocket** au frontend.

## 4. Identité fédérée & sécurité

- **Identité maison (sans Keycloak)** : `identity-service` authentifie (BCrypt), émet des **JWT RS256**
  et expose un **JWKS** (`/.well-known/jwks.json`). Tous les services et le gateway font confiance à cette
  source unique (fédération par clés publiques).
- **Autorisation externalisée OPA** : politiques **Rego** — RBAC (rôle × route) au gateway et **ABAC**
  (accès au niveau objet, anti-IDOR) côté services, en **deny-by-default**.
- **Durcissement OWASP Top 10 + IDOR** mutualisé dans `libs/common` et au gateway : validation des entrées,
  en-têtes de sécurité, CORS, erreurs sans fuite, audit, **IDs UUID** (anti-énumération), garde d'accès au
  niveau objet. Détails : [`security.md`](security.md).

## 5. Données

PostgreSQL, **une base par service** ; schéma versionné par **Flyway** (DDL fidèle, types `citext`/enum/`inet`,
clés primaires UUID). Les binaires (documents, courriers, logos) sont stockés dans **MinIO** (S3), seules les
métadonnées sont en base. Détails et DDL : [`database.md`](database.md).

## 6. Qualité, build et tests

- **Build reproductible** : tout le backend Java se compile dans des conteneurs **Maven** (aucun JDK local),
  avec cache `.m2` partagé (BuildKit) ; multi-module Maven. Le frontend se build avec Angular CLI.
- **Tests unitaires** : JUnit/Mockito sur les 7 services + `libs/common` + gateway — **~120 tests, tous verts**.
- **Tests E2E** : **Playwright** sur le parcours d'authentification réel (garde de route, échec et succès de
  connexion contre le gateway) — **3/3 verts**.
- **Validation runtime** prouvée : login réel → JWT → appels protégés HTTP 200 sur les 7 services ; 401 sans
  jeton ; décisions OPA correctes.

## 7. Mise en œuvre

Orchestration **Docker Compose** (15 conteneurs). Démarrage, identifiants de démonstration, ports et commandes
de test : voir le [`README`](../README.md). Convention de travail : monorepo, **micro-commits** en français,
développement parallèle en **git worktrees** avec rebase linéaire.

## 8. Bilan et limites

**Atteint** : les trois couches communiquent réellement de bout en bout, sécurité JWT/OPA opérationnelle,
event-driven Kafka fonctionnel, frontend branché sur le gateway réel, suite de tests verte.

**Pistes d'amélioration** : jeux de **données de démonstration** pour enrichir les tableaux de bord ;
**E2E par rôle** (au-delà de l'admin) ; résolution des références (UUID → libellés) côté front ;
durcissement OPA ABAC par type de ressource ; pagination incrémentale des grandes collections.
Les obstacles techniques rencontrés et leurs solutions sont tracés dans [`journal-de-bord.md`](journal-de-bord.md).
