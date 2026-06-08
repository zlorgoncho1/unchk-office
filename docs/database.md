# Schéma de base de données — UNCHK Office

Conception du schéma PostgreSQL en architecture **une base par microservice**
(CQRS event-driven via Kafka en mode KRaft). Chaque base est **isolée** ; les
références inter-services se font par **UUID** (jamais de clé étrangère entre
bases, jamais d'appel REST entre services). Les données « possédées » par un
autre service sont répliquées localement via des **tables de projection
(read-models, suffixe `_ro` = read-only)** alimentées en consommant les topics
Kafka.

> Cohérence avec le dépôt :
> - 7 bases créées par `platform/db/init/01-init-databases.sh` :
>   `identity`, `people`, `document`, `communication`, `academic`, `insertion`, `admin`.
> - Modèle ABAC anti-IDOR conforme à `platform/opa/policies/authz.rego` :
>   une ressource expose `ownerId` et `visibility[]` à OPA (PDP), avec
>   *deny-by-default*, RBAC (rôle × route) au gateway et ABAC (objet) côté services.
> - 5 rôles : `admin`, `administratif`, `enseignant`, `appui-insertion`, `etudiant`.

---

## Conventions transverses (toutes les bases)

| Convention | Règle |
|---|---|
| **Clés primaires** | `UUID` (`DEFAULT gen_random_uuid()`, extension `pgcrypto`) — anti-énumération / anti-IDOR. |
| **Horodatage** | `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()` (mis à jour par trigger). |
| **Suppression logique** | `deleted_at TIMESTAMPTZ NULL` sur les entités métier auditables. |
| **Verrouillage optimiste** | colonne `version BIGINT NOT NULL DEFAULT 0` sur les agrégats modifiables. |
| **Read-models** | tables `*_ro`, jamais écrites par l'API ; colonne `event_offset BIGINT` / `last_event_at TIMESTAMPTZ` pour l'idempotence Kafka. |
| **Énumérations** | types `ENUM` PostgreSQL (lisibles, contraints) ; alternative `CHECK` quand l'énum doit rester ouverte. |
| **Audit minimal** | `created_by UUID` (réfère `identity.users.id`). |

Chaque base commence par les extensions communes :

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- emails insensibles à la casse
```

**Légende des références logiques** : `*_ref` / `*_id` (cross-base) = UUID
pointant vers l'entité canonique d'un autre service ; aucune contrainte FK ne
traverse une frontière de base. Validation à la lecture contre le read-model local.

---

## 1. Base `identity` — identity-service

Propriétaire de l'identité fédérée maison (JWT RS256 + JWKS). **Source de vérité
des comptes et rôles.** Topic émis : `identity.users`.

### Type énuméré

| Type | Valeurs |
|---|---|
| `role_code` | `admin`, `administratif`, `enseignant`, `appui-insertion`, `etudiant` |

### Table `users` — comptes utilisateurs (authentification)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK**, `DEFAULT gen_random_uuid()` | Identifiant opaque |
| `email` | CITEXT | NOT NULL, **UNIQUE** | Login, unique insensible à la casse |
| `password_hash` | TEXT | NOT NULL | BCrypt/Argon2 (jamais en clair) |
| `full_name` | TEXT | NOT NULL | Nom complet affiché |
| `person_ref` | UUID | NULL | → `people.students.id` OU `people.staff.id` |
| `person_kind` | TEXT | NULL, CHECK (`etudiant`,`personnel`) | Nature de l'entité liée |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Compte activé |
| `is_locked` | BOOLEAN | NOT NULL, DEFAULT FALSE | Verrouillage anti-bruteforce |
| `failed_attempts` | INT | NOT NULL, DEFAULT 0 | Compteur d'échecs |
| `last_login_at` | TIMESTAMPTZ | NULL | Dernière connexion réussie |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | Horodatage / suppression logique |

Index : `idx_users_person_ref` (partiel, `person_ref IS NOT NULL`), `idx_users_active` (partiel, `deleted_at IS NULL`).

### Table `user_roles` — affectation des rôles (cumul possible)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `user_id` | UUID | **PK**, FK → `users(id)` ON DELETE CASCADE | Utilisateur |
| `role` | `role_code` | **PK**, NOT NULL | Rôle accordé |
| `granted_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Date d'octroi |
| `granted_by` | UUID | NULL | Auteur de l'octroi |

PK composite `(user_id, role)`. Index : `idx_user_roles_role`.

### Table `signing_keys` — clés de signature JWT (rotation, exposées via JWKS)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `kid` | TEXT | NOT NULL, **UNIQUE** | Key id présent dans l'en-tête JWT |
| `algorithm` | TEXT | NOT NULL, DEFAULT `RS256` | Algorithme de signature |
| `public_pem` | TEXT | NOT NULL | Exposé via `/.well-known/jwks.json` |
| `private_pem` | TEXT | NOT NULL | **Secret**, jamais exposé |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Une seule clé active pour signer |
| `rotated_at` | TIMESTAMPTZ | NULL | Date de rotation |
| `created_at` | TIMESTAMPTZ | NOT NULL | — |

Index unique partiel `uq_signing_keys_active` sur `is_active WHERE is_active` (garantit **une seule** clé active).

### Table `refresh_tokens` — révocation / anti-rejeu

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `user_id` | UUID | NOT NULL, FK → `users(id)` ON DELETE CASCADE | Propriétaire |
| `token_hash` | TEXT | NOT NULL, **UNIQUE** | Hash du token (jamais le token brut) |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Expiration |
| `revoked_at` | TIMESTAMPTZ | NULL | Révocation explicite |
| `created_at` | TIMESTAMPTZ | NOT NULL | — |

Index : `idx_refresh_user`.

### Table `auth_audit` — journal d'authentification (OWASP : traçabilité)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `user_id` | UUID | NULL | Utilisateur concerné |
| `event` | TEXT | NOT NULL | `LOGIN_OK`, `LOGIN_FAIL`, `LOGOUT`, `LOCK`… |
| `ip_address` | INET | NULL | Adresse IP |
| `user_agent` | TEXT | NULL | Agent client |
| `occurred_at` | TIMESTAMPTZ | NOT NULL | Horodatage |

Index : `idx_auth_audit_user (user_id, occurred_at DESC)`.

### Schéma relationnel — `identity`

```
users (1) ──< user_roles (N)          [un user cumule N rôles]
users (1) ──< refresh_tokens (N)
users (1) ──< auth_audit (N, user_id NULL possible)
signing_keys                          [autonome, clés JWKS]

Références sortantes (UUID logique, hors base) :
  users.person_ref ─→ people.students.id | people.staff.id
```

---

## 2. Base `people` — people-service

**Entités CANONIQUES** Étudiant et Personnel/Formateur, référencées par UUID
partout ailleurs (Académique, Administration RH, Insertion, Communication).
Topics émis : `people.students`, `people.staff`.

### Types énumérés

| Type | Valeurs |
|---|---|
| `genre` | `homme`, `femme`, `autre` |
| `staff_kind` | `enseignant`, `enseignant_associe`, `responsable_formation`, `tuteur`, `administratif`, `appui_insertion` |

### Table `students` — ÉTUDIANT (canonique)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | Identifiant canonique étudiant |
| `ine` | VARCHAR(32) | NOT NULL, **UNIQUE** | Identifiant National Étudiant |
| `matricule` | VARCHAR(32) | **UNIQUE** | Matricule interne UNCHK |
| `first_name` / `last_name` | TEXT | NOT NULL | Identité |
| `gender` | `genre` | NOT NULL | Genre |
| `birth_date` / `birth_place` | DATE / TEXT | NULL | Naissance |
| `email` | CITEXT | NULL | Courriel |
| `phone` | VARCHAR(32) | NULL | Téléphone |
| `address` | TEXT | NULL | Adresse |
| `photo_object_key` | TEXT | NULL | Réf MinIO (bucket avatars) |
| `formation_ref` | UUID | NULL | → `academic.formations.id` (réf logique) |
| `promotion` | VARCHAR(32) | NULL | Ex « 2023-2024 » |
| `enrollment_year` / `exit_year` | SMALLINT | NULL | Années de début / sortie |
| `status` | TEXT | NOT NULL, DEFAULT `inscrit`, CHECK (`inscrit`,`diplome`,`abandon`,`suspendu`) | Statut |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | Horodatage / suppression logique |

Index : `idx_students_name (last_name, first_name)`, `idx_students_formation`, `idx_students_promo`.

### Table `student_diplomas` — diplômes obtenus

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `student_id` | UUID | NOT NULL, FK → `students(id)` ON DELETE CASCADE | Étudiant |
| `label` | TEXT | NOT NULL | Intitulé du diplôme |
| `level` | TEXT | NULL | Licence, master… |
| `obtained_at` | DATE | NULL | Date d'obtention |
| `created_at` | TIMESTAMPTZ | NOT NULL | — |

Index : `idx_diplomas_student`.

### Table `staff` — PERSONNEL / FORMATEUR (canonique)

Couvre enseignants, associés, responsables de formation, tuteurs, administratifs, appui-insertion.

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | Identifiant canonique personnel |
| `matricule` | VARCHAR(32) | **UNIQUE** | Matricule |
| `first_name` / `last_name` | TEXT | NOT NULL | Identité |
| `gender` | `genre` | NOT NULL | Genre |
| `kind` | `staff_kind` | NOT NULL | Type de personnel |
| `email` | CITEXT | NULL | Courriel |
| `phone` | VARCHAR(32) | NULL | Téléphone |
| `grade` | TEXT | NULL | Grade / fonction |
| `speciality` | TEXT | NULL | Spécialité (formateur) |
| `department` | TEXT | NULL | Département |
| `photo_object_key` | TEXT | NULL | Réf MinIO |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | En activité |
| `hired_at` | DATE | NULL | Date d'embauche |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | Horodatage / suppression logique |

Index : `idx_staff_name (last_name, first_name)`, `idx_staff_kind` (partiel, `deleted_at IS NULL`).

### Schéma relationnel — `people`

```
students (1) ──< student_diplomas (N)
staff                                   [autonome, entité canonique]

Références sortantes (UUID logique, hors base) :
  students.formation_ref ─→ academic.formations.id

Références entrantes (consommé ailleurs via Kafka people.students / people.staff) :
  students.id ─→ insertion.*  / communication.* / identity.users.person_ref
  staff.id    ─→ academic.* / communication.* / admin.* / insertion.* / identity.users.person_ref
```

---

## 3. Base `document` — document-service

Gestion documentaire **mutualisée** (Communication ET Administration s'appuient
dessus). Stockage binaire dans **MinIO** ; la base ne garde que les métadonnées
+ la clé objet. Topic émis : `document.documents`.

### Type énuméré

| Type | Valeurs |
|---|---|
| `document_category` | `logo`, `compte_rendu`, `courrier`, `note_service`, `circulaire`, `rapport`, `autre` |

### Table `documents` — métadonnées (binaire dans MinIO)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `title` | TEXT | NOT NULL | Titre |
| `category` | `document_category` | NOT NULL, DEFAULT `autre` | Catégorie |
| `description` | TEXT | NULL | Description |
| `bucket` | TEXT | NOT NULL | Bucket MinIO (documents, courriers…) |
| `object_key` | TEXT | NOT NULL | Chemin objet S3 |
| `mime_type` | TEXT | NOT NULL | Type MIME |
| `size_bytes` | BIGINT | NOT NULL, CHECK ≥ 0 | Taille |
| `checksum_sha256` | CHAR(64) | NULL | Intégrité |
| `owner_id` | UUID | NOT NULL | → `identity.users.id` — **ABAC anti-IDOR** (`ownerId` OPA) |
| `is_archived` | BOOLEAN | NOT NULL, DEFAULT FALSE | Archivage |
| `source_service` | TEXT | NULL | Service producteur |
| `source_ref` | UUID | NULL | Id métier d'origine (compte rendu, courrier…) |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Contrainte : **UNIQUE (bucket, object_key)**. Index : `idx_documents_category` (partiel), `idx_documents_owner`, `idx_documents_source (source_service, source_ref)`.

### Table `document_visibility` — visibilité basée rôle (ABAC → OPA)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `document_id` | UUID | **PK**, FK → `documents(id)` ON DELETE CASCADE | Document |
| `role` | TEXT | **PK**, NOT NULL | Rôle (`admin`, `enseignant`, `etudiant`…) |

PK composite `(document_id, role)`. Index : `idx_doc_visibility_role`. Alimente le `visibility[]` exposé à OPA.

### Table `document_shares` — partage nominatif

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `document_id` | UUID | **PK**, FK → `documents(id)` ON DELETE CASCADE | Document |
| `user_id` | UUID | **PK**, NOT NULL | → `identity.users.id` |
| `can_edit` | BOOLEAN | NOT NULL, DEFAULT FALSE | Droit d'édition |
| `granted_at` | TIMESTAMPTZ | NOT NULL | Date de partage |

PK composite `(document_id, user_id)`.

### Schéma relationnel — `document`

```
documents (1) ──< document_visibility (N)   [rôles autorisés → visibility[] OPA]
documents (1) ──< document_shares     (N)   [partage nominatif user_id]

ABAC OPA : documents.owner_id = ownerId ; document_visibility.role[] = visibility[]

Références sortantes (UUID logique, hors base) :
  documents.owner_id / created_by ─→ identity.users.id
  documents.source_ref            ─→ id métier du service source
```

---

## 4. Base `communication` — communication-service

Comptes rendus (réunions, séminaires, webinaires, Conseil d'Université…),
réunions, et **notifications temps réel** (push WebSocket). Topics émis :
`communication.comptesrendus`, `communication.reunions`, `notifications`.

### Types énumérés

| Type | Valeurs |
|---|---|
| `meeting_type` | `reunion`, `seminaire`, `webinaire`, `conseil_universite`, `tutorat`, `preparation_cours`, `evaluation` |
| `meeting_status` | `planifiee`, `en_cours`, `terminee`, `annulee` |
| `notification_kind` | `compte_rendu`, `circulaire`, `note_service`, `reunion`, `courrier`, `systeme` |

### Table `reunions` — réunion / événement

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `title` | TEXT | NOT NULL | Titre |
| `type` | `meeting_type` | NOT NULL, DEFAULT `reunion` | Type |
| `description` | TEXT | NULL | Description |
| `location` | TEXT | NULL | Salle ou lien visio |
| `starts_at` | TIMESTAMPTZ | NOT NULL | Début |
| `ends_at` | TIMESTAMPTZ | NULL | Fin |
| `status` | `meeting_status` | NOT NULL, DEFAULT `planifiee` | Statut |
| `organizer_id` | UUID | NOT NULL | → `people.staff.id` (réf logique) |
| `formation_ref` | UUID | NULL | → `academic.formations.id` si liée |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Contrainte : CHECK (`ends_at IS NULL OR ends_at >= starts_at`). Index : `idx_reunions_starts`, `idx_reunions_type`.

### Table `reunion_participants` — participants

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `reunion_id` | UUID | **PK**, FK → `reunions(id)` ON DELETE CASCADE | Réunion |
| `person_ref` | UUID | **PK**, NOT NULL | Staff ou étudiant (réf logique) |
| `person_kind` | TEXT | NOT NULL, CHECK (`staff`,`student`) | Nature |
| `is_present` | BOOLEAN | NULL | Émargement |

PK composite `(reunion_id, person_ref)`.

### Table `comptes_rendus` — compte rendu

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `reunion_id` | UUID | NULL, FK → `reunions(id)` ON DELETE SET NULL | Réunion source |
| `title` | TEXT | NOT NULL | Titre |
| `type` | `meeting_type` | NOT NULL | Type de réunion |
| `body` | TEXT | NULL | Contenu rédigé |
| `document_ref` | UUID | NULL | → `document.documents.id` (PDF archivé) |
| `meeting_date` | DATE | NOT NULL | Date de réunion |
| `author_id` | UUID | NOT NULL | → `people.staff.id` |
| `is_published` | BOOLEAN | NOT NULL, DEFAULT FALSE | Publication → notifications |
| `published_at` | TIMESTAMPTZ | NULL | Date de publication |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Index : `idx_cr_reunion`, `idx_cr_date (meeting_date DESC)`, `idx_cr_pub` (partiel, `WHERE is_published`).

### Table `compte_rendu_visibility` — visibilité basée rôle

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `compte_rendu_id` | UUID | **PK**, FK → `comptes_rendus(id)` ON DELETE CASCADE | Compte rendu |
| `role` | TEXT | **PK**, NOT NULL | Rôle autorisé |

PK composite `(compte_rendu_id, role)`. Archivage filtré par rôle (ABAC).

### Table `notifications` — push WebSocket

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `recipient_id` | UUID | NOT NULL | → `identity.users.id` |
| `kind` | `notification_kind` | NOT NULL | Type |
| `title` | TEXT | NOT NULL | Titre |
| `message` | TEXT | NULL | Contenu |
| `target_service` | TEXT | NULL | Service cible (deep-link) |
| `target_ref` | UUID | NULL | Ressource cible (deep-link) |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT FALSE | Lu |
| `read_at` | TIMESTAMPTZ | NULL | Date de lecture |
| `delivered_ws` | BOOLEAN | NOT NULL, DEFAULT FALSE | Poussée via WebSocket |
| `created_at` | TIMESTAMPTZ | NOT NULL | — |

Index : `idx_notif_recipient (recipient_id, is_read, created_at DESC)`.

### Read-models (projections Kafka) de `communication`

Nécessaires car les notifications doivent connaître les destinataires (rôles/utilisateurs) et les réunions référencent staff/formations.

**`identity_user_ro`** (consomme `identity.users`) — qui notifier, par rôle :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `identity.users.id` |
| `full_name` | TEXT | NOT NULL | Nom |
| `email` | CITEXT | NULL | Courriel |
| `roles` | TEXT[] | NOT NULL, DEFAULT `{}` | Rôles (index GIN) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Actif |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event consommé |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

Index GIN : `idx_identity_user_ro_roles` sur `roles`.

**`people_staff_ro`** (consomme `people.staff`) — afficher auteur / organisateur :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `people.staff.id` |
| `full_name` | TEXT | NOT NULL | Nom |
| `kind` | TEXT | NOT NULL | Type de personnel |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

### Schéma relationnel — `communication`

```
reunions (1) ──< reunion_participants (N)
reunions (1) ──< comptes_rendus (N, ON DELETE SET NULL)
comptes_rendus (1) ──< compte_rendu_visibility (N)
notifications                              [autonome, recipient_id logique]

Read-models (lecture seule, alimentés par Kafka) :
  identity_user_ro  ◄── topic identity.users   [destinataires + rôles]
  people_staff_ro   ◄── topic people.staff     [auteur / organisateur]

Références sortantes (UUID logique, hors base) :
  reunions.organizer_id / comptes_rendus.author_id ─→ people.staff.id
  reunions.formation_ref                           ─→ academic.formations.id
  comptes_rendus.document_ref                      ─→ document.documents.id
  notifications.recipient_id                       ─→ identity.users.id
```

---

## 5. Base `academic` — academic-service

Formations (dates, type, niveau, financement, nombre de formés par genre),
emplois du temps, gestion des formateurs. Topic émis : `academic.formations`.

### Types énumérés

| Type | Valeurs |
|---|---|
| `formation_level` | `certificat`, `licence`, `master`, `doctorat`, `formation_continue` |
| `formation_kind` | `initiale`, `continue`, `professionnelle`, `diplomante`, `qualifiante` |
| `funding_kind` | `etat`, `partenaire`, `autofinancement`, `projet`, `mixte` |
| `weekday` | `lundi`, `mardi`, `mercredi`, `jeudi`, `vendredi`, `samedi`, `dimanche` |

### Table `formations`

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `code` | VARCHAR(32) | **UNIQUE** | Code formation |
| `label` | TEXT | NOT NULL | Intitulé |
| `level` | `formation_level` | NOT NULL | Niveau |
| `kind` | `formation_kind` | NOT NULL, DEFAULT `initiale` | Type |
| `funding` | `funding_kind` | NULL | Financement |
| `start_date` / `end_date` | DATE | NULL | Période |
| `trained_male` | INT | NOT NULL, DEFAULT 0, CHECK ≥ 0 | **Formés (hommes)** — stats genre |
| `trained_female` | INT | NOT NULL, DEFAULT 0, CHECK ≥ 0 | **Formés (femmes)** — stats genre |
| `responsible_ref` | UUID | NULL | → `people.staff.id` (responsable) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Active |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Contrainte : CHECK (`end_date IS NULL OR start_date IS NULL OR end_date >= start_date`). Index : `idx_formations_level`.

### Table `formation_formateurs` — affectation des formateurs

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `formation_id` | UUID | **PK**, FK → `formations(id)` ON DELETE CASCADE | Formation |
| `formateur_ref` | UUID | **PK**, NOT NULL | → `people.staff.id` |
| `module` | TEXT | **PK** | Matière enseignée |
| `assigned_at` | TIMESTAMPTZ | NOT NULL | Date d'affectation |

PK composite `(formation_id, formateur_ref, module)`.

### Table `schedule_slots` — emploi du temps (créneaux)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `formation_id` | UUID | NOT NULL, FK → `formations(id)` ON DELETE CASCADE | Formation |
| `course_label` | TEXT | NOT NULL | Intitulé du cours |
| `formateur_ref` | UUID | NULL | → `people.staff.id` |
| `day_of_week` | `weekday` | NULL | Récurrent hebdo |
| `session_date` | DATE | NULL | Ou date ponctuelle |
| `start_time` / `end_time` | TIME | NOT NULL | Horaires |
| `room` | TEXT | NULL | Salle ou lien visio |
| `created_at` / `updated_at` | TIMESTAMPTZ | — | — |

Contraintes : CHECK (`end_time > start_time`), CHECK (`day_of_week IS NOT NULL OR session_date IS NOT NULL`). Index : `idx_slots_formation`, `idx_slots_formateur`, `idx_slots_date`.

### Read-model (projection Kafka) de `academic`

**`academic_formateur_ro`** (consomme `people.staff`) — afficher les noms des formateurs sans appel REST :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `people.staff.id` |
| `full_name` | TEXT | NOT NULL | Nom |
| `kind` | TEXT | NOT NULL | Enseignant, tuteur… |
| `speciality` | TEXT | NULL | Spécialité |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Actif |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

Index : `idx_academic_formateur_ro_kind`.

### Schéma relationnel — `academic`

```
formations (1) ──< formation_formateurs (N)
formations (1) ──< schedule_slots       (N)

Read-model (lecture seule, alimenté par Kafka) :
  academic_formateur_ro ◄── topic people.staff   [noms des formateurs]

Références sortantes (UUID logique, hors base) :
  formations.responsible_ref / formation_formateurs.formateur_ref
  / schedule_slots.formateur_ref ─→ people.staff.id

Références entrantes (consommé ailleurs) :
  formations.id ─→ people.students.formation_ref / insertion.* / communication.*
```

---

## 6. Base `insertion` — insertion-service

Appui à l'insertion : stages, registre de contact, statistiques d'insertion
(auto-emploi vs emploi salarié), base de partenaires. Topic émis : `insertion.events`.

### Types énumérés

| Type | Valeurs |
|---|---|
| `partner_kind` | `entreprise`, `administration`, `ong`, `institution`, `autre` |
| `internship_status` | `prevu`, `en_cours`, `termine`, `rompu`, `valide` |
| `insertion_kind` | `emploi_salarie`, `auto_emploi`, `recherche_emploi`, `poursuite_etudes`, `sans_activite` |

### Table `partners` — partenaires (structures d'accueil)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `name` | TEXT | NOT NULL | Nom |
| `kind` | `partner_kind` | NOT NULL, DEFAULT `entreprise` | Type |
| `sector` | TEXT | NULL | Secteur d'activité |
| `contact_name` / `contact_email` / `contact_phone` | TEXT / CITEXT / VARCHAR(32) | NULL | Contact |
| `address` / `city` | TEXT | NULL | Localisation |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Actif |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Contrainte : **UNIQUE (name, city)**. Index : `idx_partners_kind`.

### Table `internships` — stages (bilan de stages)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `student_ref` | UUID | NOT NULL | → `people.students.id` (réf logique) |
| `partner_id` | UUID | NULL, FK → `partners(id)` ON DELETE SET NULL | Partenaire |
| `title` | TEXT | NOT NULL | Intitulé |
| `start_date` / `end_date` | DATE | NULL | Période |
| `status` | `internship_status` | NOT NULL, DEFAULT `prevu` | Statut |
| `tutor_ref` | UUID | NULL | → `people.staff.id` (tuteur) |
| `supervisor_name` | TEXT | NULL | Maître de stage (partenaire) |
| `report_ref` | UUID | NULL | → `document.documents.id` (rapport) |
| `grade` | NUMERIC(4,2) | NULL | Note |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Contrainte : CHECK (`end_date IS NULL OR start_date IS NULL OR end_date >= start_date`). Index : `idx_internships_student`, `idx_internships_partner`, `idx_internships_status`.

### Table `contact_log` — registre de contact (devenir des diplômés)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `student_ref` | UUID | NOT NULL | → `people.students.id` |
| `contacted_at` | DATE | NOT NULL, DEFAULT CURRENT_DATE | Date de contact |
| `channel` | TEXT | NULL | Téléphone, email, présentiel |
| `notes` | TEXT | NULL | Notes |
| `agent_ref` | UUID | NULL | → `people.staff.id` (appui-insertion) |
| `created_at` | TIMESTAMPTZ | NOT NULL | — |

Index : `idx_contact_student`.

### Table `insertion_outcomes` — situation d'insertion (stats)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `student_ref` | UUID | NOT NULL | → `people.students.id` |
| `formation_ref` | UUID | NULL | → `academic.formations.id` (pour stats) |
| `kind` | `insertion_kind` | NOT NULL | **Auto-emploi vs salarié** etc. |
| `employer_name` | TEXT | NULL | Employeur / auto-entreprise |
| `job_title` | TEXT | NULL | Intitulé du poste |
| `observed_at` | DATE | NOT NULL, DEFAULT CURRENT_DATE | Date de constat (suivi à N mois) |
| `is_current` | BOOLEAN | NOT NULL, DEFAULT TRUE | Situation courante |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` | TIMESTAMPTZ | — | — |

Index : `idx_outcomes_student`, `idx_outcomes_kind`, `idx_outcomes_formation` (partiel, `WHERE is_current`).

### Read-models (projections Kafka) de `insertion`

Pour produire les statistiques (par formation, par genre) sans appel REST.

**`people_student_ro`** (consomme `people.students`) :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `people.students.id` |
| `full_name` | TEXT | NOT NULL | Nom |
| `gender` | TEXT | NOT NULL | Genre (stats) |
| `formation_ref` | UUID | NULL | Formation |
| `promotion` | VARCHAR(32) | NULL | Promotion |
| `exit_year` | SMALLINT | NULL | Année de sortie |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

Index : `idx_people_student_ro_formation`.

**`academic_formation_ro`** (consomme `academic.formations`) — libellé pour les stats :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `academic.formations.id` |
| `label` | TEXT | NOT NULL | Intitulé |
| `level` | TEXT | NOT NULL | Niveau |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

### Schéma relationnel — `insertion`

```
partners (1) ──< internships (N, ON DELETE SET NULL)
internships          [student_ref / tutor_ref / report_ref logiques]
contact_log          [student_ref / agent_ref logiques]
insertion_outcomes   [student_ref / formation_ref logiques]

Read-models (lecture seule, alimentés par Kafka) :
  people_student_ro     ◄── topic people.students     [genre, formation → stats]
  academic_formation_ro ◄── topic academic.formations [libellé → stats]

Références sortantes (UUID logique, hors base) :
  *.student_ref  ─→ people.students.id
  *.tutor_ref / agent_ref ─→ people.staff.id
  *.formation_ref ─→ academic.formations.id
  internships.report_ref  ─→ document.documents.id
```

---

## 7. Base `admin` — admin-service

Administration : courrier arrivé/départ, notes de service, circulaires, gestion
budgétaire (projet de budget, budget réalisé). Topic émis : `admin.budget`.

### Types énumérés

| Type | Valeurs |
|---|---|
| `mail_direction` | `arrive`, `depart` |
| `mail_status` | `recu`, `en_traitement`, `traite`, `archive`, `clos` |
| `admin_doc_kind` | `note_service`, `circulaire` |
| `budget_status` | `projet`, `vote`, `en_execution`, `cloture` |

### Table `mails` — courrier (arrivé / départ)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `reference` | VARCHAR(64) | **UNIQUE** | Numéro chrono d'enregistrement |
| `direction` | `mail_direction` | NOT NULL | Arrivé / départ |
| `subject` | TEXT | NOT NULL | Objet |
| `correspondent` | TEXT | NOT NULL | Expéditeur (arrivé) / destinataire (départ) |
| `mail_date` | DATE | NOT NULL | Date du courrier |
| `registered_at` | DATE | NOT NULL, DEFAULT CURRENT_DATE | Date d'enregistrement |
| `status` | `mail_status` | NOT NULL, DEFAULT `recu` | Statut |
| `assigned_to` | UUID | NULL | → `people.staff.id` (agent en charge) |
| `document_ref` | UUID | NULL | → `document.documents.id` (scan/PDF) |
| `notes` | TEXT | NULL | Notes |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Index : `idx_mails_direction (direction, mail_date DESC)`, `idx_mails_status`.

### Table `admin_communiques` — notes de service & circulaires

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `kind` | `admin_doc_kind` | NOT NULL | Note de service / circulaire |
| `reference` | VARCHAR(64) | **UNIQUE** | Référence |
| `title` | TEXT | NOT NULL | Titre |
| `body` | TEXT | NULL | Contenu |
| `document_ref` | UUID | NULL | → `document.documents.id` |
| `issue_date` | DATE | NOT NULL, DEFAULT CURRENT_DATE | Date d'émission |
| `is_published` | BOOLEAN | NOT NULL, DEFAULT FALSE | Publication → notifications |
| `published_at` | TIMESTAMPTZ | NULL | Date de publication |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMPTZ | — | — |

Index : `idx_communiques_kind (kind, issue_date DESC)`.

### Table `communique_targets` — ciblage par rôle

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `communique_id` | UUID | **PK**, FK → `admin_communiques(id)` ON DELETE CASCADE | Communiqué |
| `role` | TEXT | **PK**, NOT NULL | Rôle destinataire |

PK composite `(communique_id, role)`. Détermine qui reçoit la notification.

### Table `budgets` — budget (projet + réalisé)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `fiscal_year` | SMALLINT | NOT NULL | Exercice |
| `label` | TEXT | NOT NULL | Libellé |
| `status` | `budget_status` | NOT NULL, DEFAULT `projet` | Statut |
| `total_planned` | NUMERIC(16,2) | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Total prévu |
| `total_realized` | NUMERIC(16,2) | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Total réalisé |
| `currency` | CHAR(3) | NOT NULL, DEFAULT `XOF` | Devise |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Verrou optimiste |
| `created_by` | UUID | NOT NULL | Auteur |
| `created_at` / `updated_at` | TIMESTAMPTZ | — | — |

Contrainte : **UNIQUE (fiscal_year, label)**. Index : `idx_budgets_year`.

### Table `budget_lines` — lignes budgétaires (prévu vs réalisé par poste)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | — |
| `budget_id` | UUID | NOT NULL, FK → `budgets(id)` ON DELETE CASCADE | Budget |
| `category` | TEXT | NOT NULL | Poste de dépense/recette |
| `direction` | TEXT | NOT NULL, CHECK (`depense`,`recette`) | Sens |
| `planned_amount` | NUMERIC(16,2) | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Montant prévu |
| `realized_amount` | NUMERIC(16,2) | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Montant réalisé |
| `label` | TEXT | NULL | Libellé |
| `created_at` / `updated_at` | TIMESTAMPTZ | — | — |

Index : `idx_budget_lines_budget`.

### Read-model (projection Kafka) de `admin`

**`people_staff_ro`** (consomme `people.staff`) — afficher l'agent en charge d'un courrier sans appel REST :

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | UUID | **PK** | = `people.staff.id` |
| `full_name` | TEXT | NOT NULL | Nom |
| `kind` | TEXT | NOT NULL | Type de personnel |
| `department` | TEXT | NULL | Département |
| `last_event_at` | TIMESTAMPTZ | NOT NULL | Dernier event |
| `event_offset` | BIGINT | NULL | Idempotence Kafka |

### Schéma relationnel — `admin`

```
admin_communiques (1) ──< communique_targets (N)   [rôles ciblés → notifications]
budgets (1) ──< budget_lines (N)
mails                  [assigned_to / document_ref logiques]

Read-model (lecture seule, alimenté par Kafka) :
  people_staff_ro ◄── topic people.staff   [agent en charge]

Références sortantes (UUID logique, hors base) :
  mails.assigned_to ─→ people.staff.id
  mails.document_ref / admin_communiques.document_ref ─→ document.documents.id
```

---

## Synthèse — propriété des tables par service

| Service / Base | Tables maîtresses (écriture) | Tables de projection (`_ro`, lecture Kafka) | Topic(s) émis |
|---|---|---|---|
| **identity-service** / `identity` | `users`, `user_roles`, `signing_keys`, `refresh_tokens`, `auth_audit` | — | `identity.users` |
| **people-service** / `people` | `students`, `student_diplomas`, `staff` | — | `people.students`, `people.staff` |
| **document-service** / `document` | `documents`, `document_visibility`, `document_shares` | — | `document.documents` |
| **communication-service** / `communication` | `reunions`, `reunion_participants`, `comptes_rendus`, `compte_rendu_visibility`, `notifications` | `identity_user_ro`, `people_staff_ro` | `communication.comptesrendus`, `communication.reunions`, `notifications` |
| **academic-service** / `academic` | `formations`, `formation_formateurs`, `schedule_slots` | `academic_formateur_ro` | `academic.formations` |
| **insertion-service** / `insertion` | `partners`, `internships`, `contact_log`, `insertion_outcomes` | `people_student_ro`, `academic_formation_ro` | `insertion.events` |
| **admin-service** / `admin` | `mails`, `admin_communiques`, `communique_targets`, `budgets`, `budget_lines` | `people_staff_ro` | `admin.budget` |

---

## Cartographie des flux Kafka (qui projette quoi)

```
              ┌──────────────── topic identity.users ───────────────┐
              ▼                                                      │
   communication.identity_user_ro                                   │
                                                                     │
   topic people.staff ──┬──► academic.academic_formateur_ro         │
                        ├──► communication.people_staff_ro          │
                        └──► admin.people_staff_ro                  │
                                                                     │
   topic people.students ──► insertion.people_student_ro            │
   topic academic.formations ──► insertion.academic_formation_ro    │
                                                                     │
   (identity émet ; ne consomme aucun read-model) ──────────────────┘
```

> Chaque consommateur applique l'idempotence via `event_offset` / `last_event_at`
> et n'écrit **jamais** dans une table `_ro` autrement que par projection Kafka.

---

## Notes de conception clés

- **Anti-IDOR / anti-énumération** : toutes les PK sont des `UUID` ; les colonnes
  `owner_id` / `*_visibility` / `*_targets` alimentent l'ABAC OPA décrit dans
  `platform/opa/policies/authz.rego`. Une ressource expose `ownerId`
  (`documents.owner_id`) et `visibility[]` (rôles de `document_visibility` /
  `compte_rendu_visibility`) au PDP, en *deny-by-default* (`default allow := false`).
- **CQRS strict** : aucune FK ne traverse une frontière de base. Les `*_ref` sont
  des UUID logiques validés à la lecture contre les read-models locaux ;
  `event_offset` garantit l'idempotence de la consommation Kafka. **Zéro appel REST
  entre services** (seul canal : Kafka KRaft, sans ZooKeeper).
- **Entités canoniques** : `people.students` et `people.staff` sont les seules
  sources de vérité pour Étudiant et Personnel/Formateur ; tout le reste les
  référence par UUID et en projette une copie lecture seule (`*_ro`).
- **Notifications** : `comptes_rendus.is_published`, `admin_communiques.is_published`
  et la table `notifications` matérialisent la règle « notification automatique sur
  nouveau compte rendu / circulaire » : publication → event Kafka → projection des
  destinataires (`identity_user_ro` filtré par rôle via `*_targets` / `*_visibility`)
  → insert `notifications` → push WebSocket (`delivered_ws`).
- **MinIO** : le binaire n'est jamais en base ; seules les métadonnées et le couple
  `(bucket, object_key)` (unique) sont stockés dans `document.documents`.
  Les références objet (logos, scans, rapports, PDF de comptes rendus) passent par
  `document_ref` / `report_ref` / `photo_object_key`.
- **Statistiques / export PDF-Excel** : `formations.trained_male` / `trained_female`
  et `insertion_outcomes.kind` couvrent directement les stats par genre et
  auto-emploi vs salarié exigées par l'énoncé, projetées via `people_student_ro`
  et `academic_formation_ro` côté insertion.
- **Sécurité OWASP** : `auth_audit` (traçabilité), `is_locked` / `failed_attempts`
  (anti-bruteforce), `refresh_tokens.token_hash` (hash, jamais le token brut),
  `signing_keys` (rotation JWT RS256 + JWKS).

### Fichiers de référence du dépôt

- `platform/db/init/01-init-databases.sh` — création des 7 bases.
- `platform/opa/policies/authz.rego` — modèle ABAC `ownerId` / `visibility[]`, RBAC route × rôle, *deny-by-default*.
- `CLAUDE.md` — modules fonctionnels et entités canoniques (Étudiant, Personnel/Formateur).
- `README.md` — topics Kafka et principe CQRS (read-model local par service).
