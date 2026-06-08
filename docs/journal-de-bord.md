# Journal de bord — UNCHK Office

Suivi chronologique des **problèmes rencontrés** et des **solutions appliquées**, tenu au fil de l'avancement.
Objectif : tracer les décisions et éviter de re-buter sur les mêmes obstacles.

| # | Date | Problème rencontré | Cause | Solution appliquée |
|---|------|--------------------|-------|--------------------|
| 1 | 2026-06-08 | Choix du mode Kafka hésitant (ZooKeeper vs KRaft) | Consigne d'abord « pas KRaft », puis recadrée « sans ZooKeeper » | Adoption de **KRaft** (cluster natif), nœud combiné `broker,controller`, extensible en cluster 3 nœuds |
| 2 | 2026-06-08 | Impossible de compiler le backend en local | Pas de **JDK ni Maven** installés (seulement Node + Docker) | Compilation Java **déléguée à un conteneur Maven** (`maven:3.9-eclipse-temurin-21`), cache `.m2` monté en volume |
| 3 | 2026-06-08 | `ng new` échoue : « Angular CLI requires Node ≥ v24.15.0 » | Angular **20** (latest) exige Node 24.15, or l'environnement est en **24.10** | **Pin d'Angular 19** (pleinement capable, compatible Node 24.10 — simples avertissements `EBADENGINE`) |
| 4 | 2026-06-08 | Générateur `m3-theme` : `Unknown arguments` puis fichier mal rangé | Options en **dash-case** (`--primary-color`…) et `--directory` collé comme **préfixe** du nom de fichier | Bons flags + déplacement manuel du fichier vers `src/styles/_theme-colors.scss` |
| 5 | 2026-06-08 | `.dockerignore` de module inopérants et non conformes | Contexte de build = **racine** (Docker ne lit que le `.dockerignore` racine) ; l'un **nommait** `CLAUDE.md`/PDF (interdit) | Suppression des `.dockerignore` de module, création d'un **`.dockerignore` racine conforme** (exclut le lourd, ne nomme aucun fichier interdit) |
| 6 | 2026-06-08 | `ng build` échoue : `@angular/animations/browser` introuvable | `provideAnimationsAsync()` requiert le paquet **`@angular/animations`**, non installé par le scaffold ni par `ng add @angular/material` | `npm install @angular/animations@^19` |
| 7 | 2026-06-08 | Compilation des services : `EntityName() has protected access` (Document, Budget, Notification, Partner…) | Constructeurs sans-argument JPA déclarés `protected`, mais appelés via `new Entity()` depuis le package `service` (hors package `domain`) | Passage des constructeurs sans-argument concernés en `public` (13 entités/projections) |
| 8 | 2026-06-08 | `insertion-service` test : `no suitable method found for thenReturn(List<Object>)` | Piège varargs Java : `List.of(new Object[]{...})` avec **un seul** tableau est étalé en `List<Object>` au lieu de `List<Object[]>` | Témoin de type explicite `List.<Object[]>of(...)` |
| 9 | 2026-06-08 | Conventions de config **divergentes** entre services (DB_URL vs SPRING_DATASOURCE_URL vs DB_HOST ; issuer JWT `unchk-office` vs URL ; identity sur 8081 vs JWKS attendu sur 8080) | 7 agents indépendants, conventions non harmonisées | Normalisation via un bloc d'env partagé dans `docker-compose` (identity exposé en 8080, identifiants DB unifiés, `JWT_ISSUER=unchk-office`) — affinage runtime à la mise en route |

> Ce journal est mis à jour à chaque nouvel obstacle non trivial. Voir aussi `docs/architecture.md`,
> `docs/security.md` et `docs/database.md` pour les décisions de conception.
