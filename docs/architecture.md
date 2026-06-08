# Architecture event-driven — UNCHK Office

Plateforme de gestion administrative et pédagogique de l'**Université Cheikh Hamidou Kane**.
Monorepo / microservices **event-driven**. Ce document décrit la topologie, le bus Kafka, le
principe CQRS / read-models locaux, la sécurité fédérée (JWT/JWKS + OPA), le flux d'une requête,
le flux de notification temps réel et la procédure de build/run via Docker.

> Référentiel de vérité : `README.md`, `docker-compose.yml`,
> `platform/opa/policies/authz.rego`, `platform/db/init/01-init-databases.sh`,
> `brand/design-tokens.json`. Ce document ne fait que les expliciter et les relier ; en cas
> d'écart, ces fichiers font foi.

---

## 1. Principes d'architecture (non négociables)

1. **Le gateway est le seul point d'entrée REST.** Frontend → gateway = REST + WebSocket ;
   gateway → services = REST.
2. **Inter-services = 100% Apache Kafka** (mode KRaft, **sans ZooKeeper**). **Aucun appel REST
   entre microservices.**
3. **CQRS / read-models locaux** : chaque service maintient ses propres projections en consommant
   les topics des autres services. Il ne fait jamais de `GET` vers un service voisin.
4. **Une base PostgreSQL par service** (`identity`, `people`, `document`, `communication`,
   `academic`, `insertion`, `admin`).
5. **Identité fédérée maison** : `identity-service` émet des JWT **RS256** et expose un endpoint
   **JWKS** ; tous les autres composants valident les jetons via ce JWKS (pas de Keycloak).
6. **Autorisation externalisée OPA** (Rego) : **RBAC** grossier (rôle × route) au gateway +
   **ABAC** fin (accès au niveau objet, anti-IDOR) côté services. *Deny-by-default.*
7. **Clés primaires UUID partout** (anti-énumération). Aucun identifiant séquentiel exposé.
8. **Stockage objet MinIO (S3)** pour logos, avatars, documents, courriers, comptes rendus.

Rôles applicatifs (5) : `admin`, `administratif`, `enseignant`, `appui-insertion`, `etudiant`.
`admin` = tous droits (codé dans `authz.rego` : `allow if "admin" in input.subject.roles`).

---

## 2. Topologie (diagramme ASCII)

```
                         ┌──────────────────────────────────────────────┐
                         │          NAVIGATEUR (Frontend Angular)        │
                         │   Angular Material · charte UNCHK · Iconify   │
                         └───────────────┬───────────────▲──────────────┘
                                         │               │
                          REST (HTTPS)   │               │  WebSocket (push notifications)
                                         │               │
                ┌────────────────────────▼───────────────┴───────────────────────┐
                │             API GATEWAY  (Spring Cloud Gateway / WebFlux)        │
                │  SEUL point d'entrée REST  ·  tier Middle                        │
                │  Filtres globaux :                                               │
                │   - SecureHeaders (CSP, HSTS, X-Frame-Options, nosniff…)         │
                │   - CORS liste blanche · RateLimiter · X-Correlation-Id          │
                │   - JwtAuthGlobalFilter   → valide JWT via JWKS (RS256)          │
                │   - OpaAuthorizationFilter→ RBAC route (rôle × méthode × path)   │
                │   - JwtAuthWebSocket      → valide JWT au handshake /ws          │
                └───┬───────┬───────┬───────┬───────┬───────┬───────┬─────────────┘
                    │ REST  │ REST  │ REST  │ REST  │ REST  │ REST  │ REST
                    ▼       ▼       ▼       ▼       ▼       ▼       ▼
              ┌─────────┐┌───────┐┌────────┐┌────────────┐┌────────┐┌─────────┐┌───────┐
              │identity ││people ││document││communication││academic││insertion││ admin │
              │  :8081  ││ :8082 ││ :8083  ││   :8084     ││  :8085 ││  :8086  ││ :8087 │
              │ (JWT/   ││Étud./ ││ +MinIO ││ +WebSocket  ││Format. ││ Suivi   ││Budget │
              │  JWKS)  ││Pers.  ││        ││ +Notifs     ││ EDT    ││insertion││       │
              └────┬────┘└───┬───┘└───┬────┘└──────┬──────┘└───┬────┘└────┬────┘└───┬───┘
                   │ ABAC    │ ABAC   │ ABAC       │ ABAC      │ ABAC     │ ABAC    │ ABAC
                   ▼ (OPA)   ▼        ▼            ▼           ▼          ▼         ▼
   ┌───────────────────────────────────────────────────────────────────────────────────┐
   │                         OPA  (PDP Rego, package unchk.authz)  :8181                  │
   │            RBAC route (gateway)  +  ABAC objet anti-IDOR (services)                  │
   └───────────────────────────────────────────────────────────────────────────────────┘

   ┌───────────────────────────────────────────────────────────────────────────────────┐
   │ ════════════════════  APACHE KAFKA (KRaft, sans ZooKeeper)  :9092  ════════════════ │
   │           SEUL bus inter-services — AUCUN REST entre microservices                  │
   │  identity.users · people.students · people.staff · academic.formations ·           │
   │  document.documents · communication.reunions · communication.comptesrendus ·       │
   │  insertion.events · admin.budget · notifications  (+ *.DLT, *-retry-n)              │
   └───────────────────────────────────────────────────────────────────────────────────┘
        ▲ produit / consomme (read-models CQRS locaux dans chaque service)

   ┌──────────────────────┐   ┌──────────────────────┐   ┌───────────────────────────┐
   │ PostgreSQL 16  :5432  │   │ MinIO (S3)  :9000/9001│   │ Spring Cloud Config Server │
   │ 1 base / service :    │   │ buckets :             │   │ configuration centralisée  │
   │ identity · people ·   │   │ logos · avatars ·     │   │ (profils, secrets externes)│
   │ document · communic.· │   │ documents · courriers │   └───────────────────────────┘
   │ academic · insertion ·│   │ · comptes-rendus      │
   │ admin                 │   └──────────────────────┘
   └──────────────────────┘
```

Légende des canaux :
- **Frontend → gateway** : REST (HTTPS) + WebSocket (`/ws/notifications`).
- **Gateway → services** : REST interne (load-balanced, circuit breaker par service).
- **Service ↔ service** : **uniquement Kafka** (produit/consomme des topics, jamais d'appel REST).
- **Gateway/services → OPA** : requête de décision (RBAC au gateway, ABAC objet au service).
- **Services → PostgreSQL** : chacun sa base dédiée.
- **document-service / autres → MinIO** : stockage des binaires (le binaire ne transite jamais
  par Kafka, seules les métadonnées + la clé d'objet le font).

---

## 3. Bus Kafka et liste des topics

Kafka tourne en **KRaft** (nœud unique combiné broker + controller, extensible en cluster 3 nœuds)
avec `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"` : **tous les topics doivent être créés
explicitement** (script d'init ou `NewTopic` Spring). Réplication = 1 (mono-broker de dev),
3 partitions par défaut sauf `notifications` (6 partitions, fan-out).

### 3.1 Convention de nommage

Format `<contexte>.<aggregat>`, minuscules, point comme séparateur, **agrégat au pluriel**.

| Catégorie | Convention | Exemple |
|---|---|---|
| Topic métier (event-carried state transfer) | `<service>.<aggregat>` | `people.staff` |
| Notification transverse | `notifications` (sans préfixe, partagé) | `notifications` |
| Dead Letter Topic | `<topic-source>.DLT` | `people.staff.DLT` |
| Retry non-bloquant | `<topic-source>-retry-<n>` | `people.staff-retry-0` |

### 3.2 Liste des topics (à créer explicitement)

| Topic | Producteur | Politique | Clé de partition | Rôle |
|---|---|---|---|---|
| `identity.users` | identity-service | `compact` | `userId` | Comptes, rôles, statut (jamais de hash/mot de passe). |
| `people.students` | people-service | `compact` | `studentId` | Étudiant **canonique**. |
| `people.staff` | people-service | `compact` | `staffId` | Personnel / Formateur **canonique**. |
| `academic.formations` | academic-service | `compact` | `formationId` | Formations, financement, formés par genre. |
| `document.documents` | document-service | `delete` | `documentId` | Métadonnées + clé MinIO (binaire dans MinIO). |
| `communication.reunions` | communication-service | `delete` | `reunionId` | Réunions (Conseil, tutorat, évaluations…). |
| `communication.comptesrendus` | communication-service | `delete` | `compteRenduId` | Comptes rendus (déclencheur de notifs). |
| `insertion.events` | insertion-service | `delete` | `studentId` | Contact, stages, statut d'emploi, partenaires. |
| `admin.budget` | admin-service | `compact` | `budgetLineId` | Budget projet / réalisé. |
| `notifications` | communication-service | `delete` (6 part.) | `recipientId` | Push temps réel vers WebSocket. |

> En complément, pour chaque topic : un `*.DLT` (dead letter) et des `*-retry-n` (retry
> non-bloquant Spring Kafka).

### 3.3 Enveloppe d'événement (headers Kafka)

L'enveloppe vit dans les **headers** (pas dans le payload, pour éviter la divergence). La **valeur**
du message ne porte que l'**état de l'agrégat** (sérialisation **JSON**, `__TypeId__` désactivé au
profit de `eventType` + `eventVersion`). Classe partagée `EventEnvelope<T>` dans `libs/common`.

| Header | Type | Rôle |
|---|---|---|
| `eventId` | UUID | Idempotence côté consommateur (table `processed_events`). |
| `eventType` | string | `Created` / `Updated` / `Deleted` ou nom métier (`CompteRenduPublie`). |
| `eventVersion` | int | Version du schéma du payload (évolutions **additives** uniquement). |
| `aggregateType` | string | `Student`, `Staff`, `Formation`, `Document`… |
| `aggregateId` | UUID | UUID de l'agrégat = recopie de la clé de partition. |
| `occurredAt` | ISO-8601 UTC | Horodatage métier de l'événement. |
| `traceId` | string | Corrélation `X-Correlation-Id` du gateway → Kafka → WebSocket. |
| `producer` | string | Service producteur (`people-service`). |
| `tenantId` | UUID? | Réservé multi-tenant (mono-tenant ici, optionnel). |

Un `Deleted` porte un payload tombstone logique
`{ "id": "<uuid>", "deletedAt": "<iso>", "deletedBy": "<uuid-user>" }`. Sur les topics
**compactés**, un vrai tombstone Kafka (valeur `null`) peut purger la clé.

---

## 4. CQRS et read-models locaux

Le système applique **CQRS** au niveau inter-services : un agrégat n'a qu'**un seul propriétaire**
(le service producteur, source de vérité dans sa base), et tout autre service qui en a besoin en
maintient une **copie en lecture seule** (read-model / projection) alimentée par Kafka.

**Pourquoi** : c'est ce qui rend possible la règle « zéro REST entre services ». Quand
academic-service doit afficher les formateurs d'une formation, il lit **sa propre projection**
`read_formateur(staff_id, name, speciality, staff_type)` — jamais un `GET` vers people-service.

**Mécanique** :
1. people-service modifie un personnel → écrit dans sa base `people` **et** émet `people.staff`
   (`eventType=Updated`) via le pattern Outbox.
2. academic-service consomme `people.staff`, déduplique sur `eventId`, fait un upsert de
   `read_formateur`. Son read-model est désormais à jour, localement, sans réseau synchrone.
3. Un **nouveau** consommateur (ou une reprise) rejoue le topic **compacté** depuis l'offset 0 et
   reconstruit intégralement sa projection (dernier état par clé).

### Matrice producteur → consommateurs (synthèse)

| Topic | Producteur | Consommateurs (read-model construit) |
|---|---|---|
| `identity.users` | identity | people, communication, document, academic, insertion, admin, gateway/OPA |
| `people.students` | people | academic, insertion, communication, document, admin |
| `people.staff` | people | **academic (formateurs sans REST)**, communication, insertion, document, admin |
| `academic.formations` | academic | people, insertion, communication, admin |
| `document.documents` | document | communication (→ notif circulaire), admin, OPA (ABAC) |
| `communication.reunions` | communication | communication (self → notif), academic, admin |
| `communication.comptesrendus` | communication | communication (self → notif), document, admin |
| `insertion.events` | insertion | admin (stats), communication (suivi), people |
| `admin.budget` | admin | academic, communication (optionnel) |
| `notifications` | communication | communication / WebSocket Push (self) → front via gateway |

---

## 5. Sécurité fédérée — JWT / JWKS + OPA

### 5.1 Identité fédérée maison (RS256 + JWKS)

- `identity-service` est l'**autorité d'émission**. À la connexion (`POST /api/auth/login`), il
  vérifie les identifiants et délivre un **JWT signé RS256** (+ refresh token).
- Il expose les clés **publiques** via JWKS : `GET /api/auth/.well-known/jwks.json`.
- **Aucun autre composant ne détient la clé privée.** Gateway et services valident la signature,
  l'`issuer`, l'`audience` et l'`expiry` **en récupérant le JWKS** (cache + rotation des clés).
- Routes publiques en **liste blanche** au gateway (pas de JWT, pas d'OPA) :
  `/api/auth/login`, `/api/auth/refresh`, `/api/auth/.well-known/jwks.json`.
- Le topic `identity.users` propage rôles et statut (`ACTIVE`/`SUSPENDED`/`DISABLED`) → révocation
  rapide via le read-model côté gateway/OPA. **Aucun secret/hash ne transite par Kafka.**

### 5.2 Deux niveaux d'autorisation OPA (package `unchk.authz`)

OPA est le **PDP** unique. Entrée type (cf. `authz.rego`) :

```json
{
  "subject":  {"id": "u-123", "roles": ["enseignant"]},
  "action":   "read | create | update | delete",
  "resource": {"type": "document", "id": "d-1", "ownerId": "u-9", "visibility": ["enseignant","admin"]},
  "request":  {"method": "GET", "path": "/api/documents/d-1"}
}
```

- **RBAC grossier au gateway** : `OpaAuthorizationFilter` interroge la règle `allow` qui teste
  `route_allowed` (rôle × méthode × path via `data.role_permissions` + `glob.match`). *Deny-by-
  default* : `default allow := false`.
- **ABAC fin au service (anti-IDOR)** : pour tout accès à un objet identifié par UUID, le service
  charge l'objet, construit le `resource` (`type`, `id`, `ownerId`, `visibility`, attributs) et
  rappelle OPA. La Rego autorise la lecture seulement si `object_visible` (un rôle du sujet ∈
  `resource.visibility`) **ou** `resource.ownerId == subject.id`.
- **Anti-énumération** : sur refus en **lecture**, le service renvoie **404** (et non 403) pour ne
  pas confirmer l'existence d'un UUID. L'appartenance au RBAC de la route ne donne **jamais** accès
  direct à un objet par son UUID : c'est le seul rempart contre l'IDOR.

### 5.3 Durcissement OWASP transverse

Mutualisé, jamais réimplémenté par service :
- **Gateway** : en-têtes de sécurité (CSP, HSTS, X-Frame-Options=DENY, X-Content-Type-Options=
  nosniff, Referrer-Policy), CORS liste blanche (origine frontend), rate-limiting, limite de taille
  de requête, `X-Correlation-Id`.
- **`libs/common`** : garde ABAC objet, validation des DTO, journalisation d'audit, gestion
  d'erreurs sans fuite d'interne, UUID opaques.

---

## 6. Flux d'une requête REST (JWT → OPA → service)

Exemple : `GET /api/documents/{id}` (cas IDOR-sensible).

```
[1] Front ──REST GET /api/documents/d-1 (Authorization: Bearer <JWT>)──► api-gateway
[2] Gateway · filtres globaux :
      - SecureHeaders, CORS, RateLimiter
      - X-Correlation-Id généré (traceId)
      - JwtAuthGlobalFilter : valide signature RS256 via JWKS (issuer/audience/expiry)
                              injecte X-Subject-Id / X-Subject-Roles
      - OpaAuthorizationFilter : POST /v1/data/unchk/authz/allow
                                 {subject, action=read, request:{method:GET, path}}
                                 → RBAC route ; si allow=false → 403 (la requête s'arrête ici)
[3] Gateway ──REST (load-balanced, CircuitBreaker=documentCB)──► document-service:8083
[4] document-service · ABAC objet (libs/common) :
      - charge le document (read-model + base)
      - POST OPA {subject, action=read,
                  resource:{type:document, id, ownerId, visibility}}
      - allow=true → renvoie les métadonnées
      - allow=false en lecture → 404 (anti-énumération)
[5] document-service ──REST──► api-gateway ──REST──► Front
```

Codes : 200/201/204 succès · 400 validation · **401** JWT invalide (gateway) · **403** OPA RBAC
deny · **404** OPA ABAC deny en lecture (anti-énumération) · 409 conflit.

---

## 7. Flux de notification temps réel (compte rendu → WebSocket)

Cas de référence : notification automatique sur nouveau compte rendu / circulaire.

```
[1] Front ──REST POST /api/communication/comptes-rendus──► api-gateway
        (JWT validé via JWKS ; OPA RBAC route ; X-Correlation-Id généré)
[2] api-gateway ──REST──► communication-service:8084
        ABAC : action=create, resource.ownerId=<user>  (anti-IDOR)
[3] communication-service :
        - persiste le compte rendu (status=BROUILLON) dans la base `communication`
        - émet  communication.comptesrendus  eventType=CompteRenduRedige   (via Outbox)
[4] Publication (PATCH .../publish) :
        - émet  communication.comptesrendus  eventType=CompteRenduPublie
[5] communication-service consomme son propre topic :
        - RÉSOUT LES DESTINATAIRES à partir de ses READ-MODELS LOCAUX (zéro REST) :
            read_user(roles) ∩ visibility[]   ∪   read_recipient par audienceFormationIds[]
          (projections alimentées par identity.users, people.*, academic.formations)
        - pour chaque destinataire → produit un message sur `notifications`
            key=recipientId, eventType=NotificationCreee, category=COMPTE_RENDU,
            link=/communication/comptes-rendus/<id>, traceId propagé
[6] WebSocket Push (communication-service) consomme `notifications` :
        - persiste read_notification (badge + historique)
        - si la session WS du recipientId est active → envoie le frame STOMP
[7] api-gateway relaie le frame WebSocket ──WS──► Front
        - Angular incrémente le badge cloche (solar:bell-bing-bold-duotone), toast, rafraîchit la liste
```

Points clés :
- **Aucun appel REST entre services** : la résolution des destinataires se fait sur les
  read-models locaux.
- **Idempotence** : chaque consommateur déduplique sur `eventId` (`processed_events(event_id PK,
  processed_at)`), indispensable car `notifications` peut rejouer.
- **`traceId`** propagé de bout en bout (`X-Correlation-Id` gateway → headers Kafka → frame WS).
- **WebSocket** (`/ws/notifications`) : handshake authentifié par JWT (validé via JWKS au gateway
  **avant** l'upgrade), session liée à `subject.id` → pas d'IDOR temps réel cross-utilisateur.
- Même mécanique pour **circulaire** (`document.documents` `category=CIRCULAIRE` →
  `notifications` `category=CIRCULAIRE`) et **convocation réunion** (`communication.reunions`
  `ReunionPlanifiee` → `notifications` `category=CONVOCATION_REUNION` vers `participantIds`).

---

## 8. Garanties techniques

| Sujet | Choix |
|---|---|
| Atomicité écriture DB + publication Kafka | **Transactional Outbox** par service (table `outbox`, relais → Kafka). |
| Ordre | Garanti **par clé de partition** (UUID de l'agrégat / du destinataire). |
| Idempotence consommateur | Table `processed_events(event_id)` + upsert du read-model. |
| Reprise / nouveau consommateur | Topics d'état **compactés** rejoués depuis l'offset 0. |
| Erreurs | Retry non-bloquant `*-retry-n` puis **DLT** `<topic>.DLT`. |
| Évolution de schéma | `eventVersion` dans les headers ; évolutions **additives** uniquement. |
| Sécurité | Aucun secret/hash/JWT dans les payloads ; UUID opaques ; `visibility`/`ownerId` → ABAC. |

---

## 9. Build et exécution via Docker

> **Règle de build** : les microservices se construisent **via Docker (image Maven)**, **pas** de
> Maven local. L'infrastructure (Kafka, PostgreSQL, MinIO, OPA) est orchestrée par
> `docker-compose.yml`.

### 9.1 Démarrer l'infrastructure

```bash
cp .env.example .env
docker compose up -d kafka postgres minio opa kafka-ui
```

Effets à l'initialisation :
- **PostgreSQL** exécute `platform/db/init/01-init-databases.sh` → crée les 7 bases
  (`identity`, `people`, `document`, `communication`, `academic`, `insertion`, `admin`).
- **MinIO** : le job `minio-init` (image `minio/mc`) crée les buckets
  `logos`, `documents`, `courriers`, `comptes-rendus`, `avatars` (et rend `logos` public en lecture).
- **OPA** charge les politiques de `platform/opa/policies` (package `unchk.authz`).
- **Kafka** démarre en KRaft ; `auto.create.topics` est **désactivé** → les topics sont créés
  explicitement par les services (`NewTopic`) ou un script d'init.

### 9.2 Ports d'infrastructure

| Composant | URL locale |
|---|---|
| PostgreSQL | `localhost:5432` |
| Kafka (broker, listener EXTERNAL) | `localhost:9092` |
| Kafka UI | `http://localhost:8085` |
| MinIO API / Console | `http://localhost:9000` / `http://localhost:9001` |
| OPA | `http://localhost:8181` |

### 9.3 Ports applicatifs (cible)

| Service | Port | Topics produits |
|---|---|---|
| api-gateway | 8080 | — (seul point d'entrée REST + WS) |
| identity-service | 8081 | `identity.users` |
| people-service | 8082 | `people.students`, `people.staff` |
| document-service | 8083 | `document.documents` (+ MinIO) |
| communication-service | 8084 | `communication.comptesrendus`, `communication.reunions`, `notifications` (+ WS) |
| academic-service | 8085 | `academic.formations` |
| insertion-service | 8086 | `insertion.events` |
| admin-service | 8087 | `admin.budget` |

> Note : Kafka UI partage le port host `8085` en phase infra ; academic-service écoute `8085`
> **dans le réseau Docker** (`unchk-net`). Aligner le mapping des ports lors de l'ajout des
> services applicatifs au compose pour éviter le conflit côté host.

### 9.4 Build d'un service via Docker (image Maven)

Construction sans Maven local (image officielle Maven + JDK 21) :

```bash
# Build de l'artefact dans un conteneur Maven (exemple people-service)
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 \
  mvn -q -pl services/people-service -am package

# Puis construction de l'image du service (Dockerfile multi-stage recommandé)
docker compose build people-service
docker compose up -d people-service
```

Spring Boot 3.3.4 / Java 21, Spring Cloud 2023.0.3. La configuration est centralisée par le
**Config Server** (Spring Cloud Config) ; les services se connectent à Kafka via le listener
interne `kafka:19092` sur le réseau `unchk-net`.

### 9.5 Frontend

Le frontend Angular (servi par nginx) est **généré par un autre processus** et n'est pas couvert
ici. Il consomme le gateway en REST + WebSocket et applique la charte UNCHK (primaire bleu
`#1C75BC`, secondaire vert `#36A93B`, accent orange `#F39200`, texte bleu marine `#16314A`,
icônes Iconify/Solar).
