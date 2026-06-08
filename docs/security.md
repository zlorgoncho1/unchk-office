# Sécurité — Durcissement OWASP Top 10 (2021) + anti-IDOR — UNCHK Office

> **Référence au dépôt réel** : contrat d'entrée OPA déjà amorcé dans
> `platform/opa/policies/authz.rego` (sujet × action × ressource × attributs), table
> `role_permissions` dans `platform/opa/policies/data.json`, gateway réactif (Spring Cloud
> Gateway, WebFlux — tier Middle, **seul point d'entrée REST**), identité maison **JWT RS256 +
> JWKS**, **OPA en PDP**, **une base PostgreSQL par service** (`platform/db/init/01-init-databases.sh`),
> **IDs UUID**. Ce document mappe chaque risque à un mécanisme concret + sa couche, puis détaille
> les 7 contrôles demandés.
>
> **Principe directeur** : la sécurité est **transverse et mutualisée**, jamais réimplémentée par
> service. Trois lieux d'application (PEP) pour un seul point de décision (PDP, OPA) :
> **Gateway** (point d'entrée), **`libs/common`** (lib embarquée dans chaque service), **OPA**
> (politiques Rego). *Deny-by-default* partout.

---

## 1. Matrice de synthèse A01..A10

| OWASP | Risque | Mécanisme concret | Couche d'implémentation |
|---|---|---|---|
| **A01 — Broken Access Control** | IDOR, escalade de privilèges, accès objet non autorisé, force browsing | RBAC (rôle × route+méthode) au gateway via OPA `route_allowed` ; **ABAC anti-IDOR au niveau objet** via OPA `object_visible` (ownerId + visibility) appelé côté service ; *deny-by-default* (`default allow := false`) ; IDs UUID opaques | **Gateway** (RBAC grossier) + **`libs/common`** garde `@AbacGuard` (ABAC fin, dans chaque service) + **OPA** (PDP Rego) |
| **A02 — Cryptographic Failures** | Clés faibles, secrets en clair, transport non chiffré | JWT **RS256** (asymétrique, signature vérifiée via JWKS, jamais HS256/secret partagé) ; secrets via `.env`/Config Server jamais en dur ; TLS terminé en amont (nginx/ingress) + HSTS ; mots de passe hachés **Argon2id/BCrypt** ; chiffrement au repos délégué à PostgreSQL/MinIO | **identity-service** (émission JWT, hash mdp) + **`libs/common`** (vérif JWKS) + **Gateway** (HSTS) |
| **A03 — Injection** | SQLi, injection NoSQL/LDAP/commande, XSS réfléchi/stocké | JPA + **requêtes paramétrées** (jamais de concaténation SQL) ; **Bean Validation** sur tous les DTO ; encodage de sortie côté Angular (auto-escaping) + CSP ; en-tête `X-Content-Type-Options: nosniff` ; pas d'`eval` côté consumers Kafka (désérialisation typée) | **`libs/common`** (Bean Validation, sanitization) + **service** (JPA paramétré) + **Gateway** (CSP, nosniff) |
| **A04 — Insecure Design** | Manque de threat modeling, contrôles absents par conception | *Deny-by-default* partout ; **CQRS** (read-models locaux isolés, **zéro appel REST inter-service** = surface réduite) ; ségrégation par bounded context + une base par service ; rate-limiting et quotas pensés dès la conception ; principe du moindre privilège dans `role_permissions` | **Architecture** (Kafka-only, 1 DB/service) + **OPA** + **Gateway** |
| **A05 — Security Misconfiguration** | En-têtes manquants, CORS permissif, stack traces exposées, défaut non durci | En-têtes de sécurité (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy) ; **CORS en liste blanche** ; désactivation bannière/erreurs verbeuses ; `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` (déjà dans `docker-compose.yml`) ; comptes par défaut interdits | **Gateway** (filtre `WebFilter` global) |
| **A06 — Vulnerable & Outdated Components** | Dépendances avec CVE connues | BOM verrouillés (**Spring Boot 3.3.4**, **Spring Cloud 2023.0.3** dans `pom.xml`) ; build via image Maven Docker reproductible ; scan **OWASP Dependency-Check / Trivy** en CI ; pin des images (`apache/kafka:3.8.1`, `openpolicyagent/opa:0.68.0-rootless`, `postgres:16-alpine`) | **Build/CI** + **Docker** + **`pom.xml` parent** |
| **A07 — Identification & Authentication Failures** | Sessions faibles, brute force, JWT mal validé | Validation JWT complète via JWKS (**issuer, audience, expiry, signature, kid**) ; rotation de clés JWKS ; rate-limiting sur `/auth/login` ; tokens à TTL court (`JWT_ACCESS_TTL_MIN=30`, `JWT_REFRESH_TTL_DAYS=7`) ; verrouillage après N échecs ; pas de session serveur (**stateless**) | **identity-service** + **`libs/common`** (filtre JWT) + **Gateway** (rate-limit login) |
| **A08 — Software & Data Integrity Failures** | Désérialisation non sûre, pipeline non vérifié, events falsifiés | Désérialisation Kafka **typée** (pas de polymorphisme ouvert/`@class`) ; vérification de signature JWT à chaque hop ; images Docker pinnées par tag ; events Kafka produits **seulement par le service propriétaire** du topic | **`libs/common`** (config Kafka serde) + **service** |
| **A09 — Security Logging & Monitoring Failures** | Pas d'audit, pas de traçabilité, fuite de PII en logs | **Journalisation d'audit** structurée (qui/quoi/quand/décision OPA) avec **identifiant de corrélation** (`X-Correlation-Id` propagé gateway → service → headers Kafka) ; logs JSON ; masquage PII ; pas de secret/JWT dans les logs | **Gateway** (génère le correlation-id) + **`libs/common`** (MDC, filtre d'audit) |
| **A10 — Server-Side Request Forgery (SSRF)** | Requêtes sortantes manipulées (ex. URL fournie par l'utilisateur) | Surface très réduite (**zéro appel REST inter-service**, tout en Kafka) ; accès MinIO/OPA par URLs **internes fixes** (`OPA_URL=http://opa:8181`, endpoint MinIO) jamais dérivées d'input utilisateur ; pas de fetch d'URL arbitraire ; validation/allow-list si un jour un webhook est ajouté | **Architecture** (Kafka-only) + **`libs/common`** (client OPA/MinIO à URL fixe) |

---

## 2. Contrôle d'accès au niveau objet — anti-IDOR (priorité 1)

Le RBAC (rôle × route) au gateway **ne suffit pas** contre l'IDOR : deux enseignants ont la même
route `GET /api/documents/{id}` autorisée, mais l'un ne doit pas lire le document de l'autre. La
défense objet se fait **dans le service**, via la garde `libs/common` qui interroge OPA avec les
attributs réels de la ressource **chargée en base**.

### 2.1 Contrat d'entrée OPA (déjà amorcé dans `authz.rego`)

```json
{
  "subject":  { "id": "8f3a...uuid", "roles": ["enseignant"] },
  "action":   "read | create | update | delete",
  "resource": {
    "type": "document",
    "id": "d1c2...uuid",
    "ownerId": "9b7e...uuid",
    "visibility": ["enseignant", "admin"],
    "serviceContext": "document-service"
  },
  "request":  { "method": "GET", "path": "/api/documents/d1c2...uuid", "correlationId": "..." }
}
```

Règles correspondantes (extrait réel du dépôt, `platform/opa/policies/authz.rego`) :

- `default allow := false` — *deny-by-default*.
- `allow if "admin" in input.subject.roles` — bypass admin.
- `allow if route_allowed` — RBAC grossier (gateway), via `data.role_permissions`.
- `allow if { input.action == "read"; object_visible }` — ABAC objet (lecture seule à ce stade).
- `object_visible if { some r in input.subject.roles; r in input.resource.visibility }` — visibilité par rôle.
- `object_visible if input.resource.ownerId == input.subject.id` — le propriétaire accède toujours.

**Amélioration recommandée** (sans casser l'existant) : étendre l'ABAC aux écritures
(`update`/`delete`). Actuellement, `object_visible` n'est mobilisé **que pour `read`**. Ajouter
dans `authz.rego` :

```rego
# Modification/suppression : réservée au propriétaire (ou admin via la règle globale).
allow if {
    input.action in {"update", "delete"}
    input.resource.ownerId == input.subject.id
}
```

### 2.2 Modèle de propriété (`ownerId`) et de visibilité par rôle

- **`ownerId` (UUID)** : colonne présente sur chaque entité métier portant un propriétaire
  (document, compte rendu, dossier étudiant, événement insertion...). Référence l'`id` du sujet
  (UUID utilisateur d'identity-service). Le propriétaire a toujours `read/update/delete` sur sa
  ressource.
- **`visibility` (liste de rôles)** : ensemble des rôles autorisés en lecture, stocké sur la
  ressource (ex. une circulaire `["administratif","enseignant","etudiant"]`). Permet l'archivage
  documentaire « à accès par rôle » exigé par le brief (cf. `CLAUDE.md`, module Communication).
- **Cas étudiant (anti-IDOR fort)** : `etudiant` n'a en RBAC que `GET /api/etudiants/me/**`
  (cf. `data.json`) — il ne peut **même pas formuler** une URL vers un autre dossier ; le service
  résout `me` → `subject.id` **côté serveur**, jamais via un `{id}` fourni par le client.

### 2.3 IDs opaques UUID (anti-énumération)

- **Clés primaires UUID** (jamais d'`int`/`bigserial` auto-incrémenté) — interdit le balayage
  `/1, /2, /3...`.
- UUID v4 (aléatoire) pour les entités exposées ; aucun ID séquentiel ne fuit dans les URLs,
  payloads ou en-têtes.
- Cohérent avec les « IDs opaques (UUID) » du `README.md` et l'objectif anti-énumération de la charte.

### 2.4 Flux complet d'une requête (défense en profondeur)

```
1. Frontend ──REST+JWT──► Gateway
2. Gateway : valide JWT (JWKS) → extrait subject{id, roles}
3. Gateway → OPA : RBAC (route × méthode)            [refuse tôt si route interdite]
4. Gateway ──REST + X-Correlation-Id + claims──► Service métier
5. Service : charge la ressource en base (ownerId, visibility)
6. Service (libs/common @AbacGuard) → OPA : ABAC objet (sujet × action × ressource réelle)
7. OPA : allow/deny → 200 ou 403/404 sobre
```

---

## 3. Validation des entrées — Bean Validation (priorité 2)

| Aspect | Choix |
|---|---|
| Mécanisme | `jakarta.validation` (Bean Validation) sur **tous les DTO d'entrée**, déclenché par `@Valid` sur les `@RequestBody` |
| Annotations | `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@Positive`, `@UUID` (custom), bornes sur enums |
| Où | **`libs/common`** : annotations custom (`@UuidValide`, `@TexteSansHtml`), `ConstraintValidator` mutualisés, et `@RestControllerAdvice` global qui transforme les `MethodArgumentNotValidException` en réponse **400 normalisée** sans fuite |
| Anti sur-affectation (mass assignment) | DTO dédiés (jamais l'entité JPA en `@RequestBody`) ; mapping explicite DTO → entité ; champs système (`id`, `ownerId`, `createdAt`) **jamais** liés depuis le corps client |
| Côté gateway | Limite de **taille de requête** (body max) et de profondeur JSON pour bloquer les payloads abusifs **avant** d'atteindre le service |
| Uploads MinIO | Validation **type MIME réel + taille max + extension allow-list** pour logos/documents/courriers/comptes-rendus/avatars (buckets définis dans `docker-compose.yml`) |

---

## 4. En-têtes de sécurité (priorité 3) — Gateway (filtre global)

Appliqués à **toutes** les réponses par un `WebFilter` global Spring Cloud Gateway (couche
**Gateway**, mutualisé pour tous les services) :

| En-tête | Valeur recommandée | Rôle |
|---|---|---|
| `Content-Security-Policy` | `default-src 'self'; img-src 'self' data: <minio>; connect-src 'self' wss://<gateway>; frame-ancestors 'none'; base-uri 'self'; object-src 'none'` | Anti-XSS, anti-clickjacking, autorise WS notifications + images MinIO |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | Force HTTPS (A02) |
| `X-Frame-Options` | `DENY` | Anti-clickjacking (redondant avec `frame-ancestors`) |
| `X-Content-Type-Options` | `nosniff` | Empêche le MIME-sniffing (A03) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limite la fuite d'URL/PII |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=()` | Réduit la surface navigateur |
| `Cache-Control` (réponses sensibles) | `no-store` | Évite la mise en cache de données privées |

> L'en-tête `Server` et toute bannière de version sont **retirés** des réponses pour ne pas
> divulguer les versions des composants.

---

## 5. CORS en liste blanche (priorité 4) — Gateway

| Aspect | Choix |
|---|---|
| Couche | **Gateway uniquement** (les services ne sont pas exposés au navigateur) |
| Origines | **Liste blanche explicite** (ex. `https://office.unchk.sn`, `http://localhost:4200` en dev) — **jamais** `*` |
| Méthodes | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| En-têtes | `Authorization, Content-Type, X-Correlation-Id` autorisés |
| `Allow-Credentials` | `true` (cookies/JWT) — incompatible avec `*`, d'où l'allow-list stricte |
| Préflight | `OPTIONS` mis en cache (`Access-Control-Max-Age`) |
| Source | Origines lues depuis la config (Config Server / `.env`), modifiables **sans recompiler** |

---

## 6. Rate-limiting au gateway (priorité 5) — Gateway

| Aspect | Choix |
|---|---|
| Couche | **Gateway** (`RequestRateLimiter` Spring Cloud Gateway, algorithme token-bucket) |
| Clé | Par **sujet JWT** (`subject.id`) pour les routes authentifiées ; par **IP** pour les routes anonymes (`/auth/**`) |
| Quotas | Quota global modéré + quota **renforcé sur `/auth/login`** (anti brute-force, lié à A07) |
| Backend | Bucket en mémoire (mono-instance) ou Redis si scale-out ; réponse **429** avec `Retry-After` |
| Complément | Limite de taille de requête + timeout amont pour amortir les abus |

---

## 7. Gestion d'erreurs sans fuite (priorité 6) — `libs/common` + Gateway

| Aspect | Choix |
|---|---|
| Couche services | **`libs/common`** : `@RestControllerAdvice` global qui mappe les exceptions vers un corps **RFC 7807** (`application/problem+json`) normalisé |
| Principe | **Aucune** stack trace, requête SQL, nom de classe, chemin interne ou message d'ORM renvoyé au client |
| Codes | `400` validation, `401` non authentifié, `403` refus OPA (message générique), `404`/`403` **indistinct** sur ressource non possédée, `409` conflit, `429` rate-limit, `500` générique |
| Anti-IDOR | Sur ressource **existante mais non autorisée**, répondre de manière **indistincte** d'une 404 pour ne pas confirmer l'existence d'un UUID |
| Corrélation | Chaque réponse d'erreur inclut le `correlationId` (côté serveur dans les logs, exposé au client pour le support sans détails internes) |
| Gateway | Page/JSON d'erreur sobre pour les erreurs amont (503/504), aucune trace de routage |

---

## 8. Journalisation d'audit + identifiant de corrélation (priorité 7) — Gateway + `libs/common`

| Aspect | Choix |
|---|---|
| Couche | **Gateway** génère `X-Correlation-Id` (UUID) si absent ; **`libs/common`** le pose dans le **MDC** (logback) et le propage |
| Propagation | Gateway → service (en-tête HTTP) → **headers Kafka** (pour tracer une action à travers les projections CQRS asynchrones) → logs |
| Contenu audit | `timestamp` (UTC), `correlationId`, `subject.id`, `roles`, `action`, `resource.type/id`, `decision OPA (allow/deny)`, `méthode`, `path`, `statut` — **format JSON structuré** |
| Événements audités | Connexion/échec, décisions OPA (surtout les **deny**), CRUD sur ressources sensibles (documents, budget, dossiers), uploads MinIO |
| Confidentialité | **Masquage PII** ; **jamais** de JWT, mot de passe ni secret en clair dans les logs (anti A09) |
| Stockage | Logs structurés collectables (stdout → agrégateur) |

---

## 9. Répartition `libs/common` vs Gateway (synthèse de placement)

| Va dans **Gateway** (WebFlux, point d'entrée, mutualisé pour tous) | Va dans **`libs/common`** (lib partagée, embarquée dans chaque service) |
|---|---|
| Validation JWT via JWKS (issuer/audience/expiry/kid) | Filtre JWT côté service (re-vérifie la signature, défense en profondeur) |
| **RBAC** grossier (rôle × route) appelé à OPA | **ABAC anti-IDOR** au niveau objet (`@AbacGuard` + client OPA) |
| En-têtes de sécurité (CSP, HSTS, X-Frame-Options, nosniff, Referrer-Policy) | **Bean Validation** : annotations custom + `ConstraintValidator` |
| **CORS** en liste blanche | **Gestion d'erreurs** RFC 7807 (`@RestControllerAdvice`) sans fuite |
| **Rate-limiting** + limite de taille de requête | **Audit** + MDC + propagation `correlationId` (HTTP + Kafka headers) |
| Génération du `X-Correlation-Id` | Serde Kafka **typée** (anti-désérialisation non sûre) ; client OPA/MinIO à URL fixe (anti-SSRF) |

> **OPA** (couche transverse, PDP Rego dans `platform/opa/policies/`) reçoit à la fois le RBAC du
> gateway et l'ABAC des services, en *deny-by-default*. C'est le **point de décision unique** ;
> gateway et `libs/common` ne sont que des **PEP** (points d'application).

---

## 10. Couverture des 5 rôles dans le modèle d'accès

Aligné sur `platform/opa/policies/data.json` (table `role_permissions`).

| Rôle | RBAC (route, cf. `data.json`) | ABAC objet (anti-IDOR) |
|---|---|---|
| `admin` | `*` sur `/**` (tous droits) | bypass `object_visible` (règle globale) |
| `administratif` | GET `/api/**` ; POST `/api/documents/**`, `/api/communication/**`, `/api/admin/**` | propriétaire + `visibility` |
| `enseignant` | GET `/api/**` ; POST `/api/academic/**`, `/api/communication/comptes-rendus/**` | ne lit que les documents dont la `visibility` contient `enseignant` ou dont il est `ownerId` |
| `appui-insertion` | GET + POST `/api/insertion/**` | propriétaire + visibilité des fiches insertion |
| `etudiant` | GET `/api/etudiants/me/**`, `/api/academic/emplois-du-temps/**` | `me` résolu **côté serveur** vers `subject.id` (jamais d'`{id}` client) |

---

## 11. Recommandations d'implémentation à venir (sans coder ici)

1. **Étendre `authz.rego`** aux actions `update`/`delete` (ABAC écriture, cf. §2.1) — actuellement
   seul `read` déclenche l'ABAC objet.
2. **Scaffolder `libs/common`** avec les modules : `jwt`, `opa` (client + `@AbacGuard`), `web`
   (en-têtes / erreurs / audit / MDC), `kafka` (serde typée) — cohérent avec la structure annoncée
   au `README.md`.
3. **Intégrer en CI** un scan de dépendances (OWASP Dependency-Check / Trivy) et le pin systématique
   des images Docker (déjà partiellement fait dans `docker-compose.yml`).
4. **Définir l'allow-list CORS et les origines** dans la config (Config Server / `.env`) avant la
   mise en démo.
