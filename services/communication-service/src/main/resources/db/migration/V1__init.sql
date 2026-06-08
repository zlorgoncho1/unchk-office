-- ============================================================
-- Base "communication" — communication-service
-- Comptes rendus (réunions, séminaires, webinaires, Conseil d'Université),
-- réunions et notifications temps réel (push WebSocket).
-- Source : docs/database.md (section 4). Clés primaires UUID (anti-IDOR).
-- ============================================================

-- Extensions communes (cf. conventions transverses des docs).
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- emails insensibles à la casse

-- ------------------------------------------------------------
-- Types énumérés
-- ------------------------------------------------------------
CREATE TYPE meeting_type AS ENUM (
    'reunion', 'seminaire', 'webinaire', 'conseil_universite',
    'tutorat', 'preparation_cours', 'evaluation'
);

CREATE TYPE meeting_status AS ENUM (
    'planifiee', 'en_cours', 'terminee', 'annulee'
);

CREATE TYPE notification_kind AS ENUM (
    'compte_rendu', 'circulaire', 'note_service', 'reunion', 'courrier', 'systeme'
);

-- ------------------------------------------------------------
-- Table reunions — réunion / événement
-- ------------------------------------------------------------
CREATE TABLE reunions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         TEXT NOT NULL,
    type          meeting_type NOT NULL DEFAULT 'reunion',
    description   TEXT,
    location      TEXT,                                   -- salle ou lien visio
    starts_at     TIMESTAMPTZ NOT NULL,
    ends_at       TIMESTAMPTZ,
    status        meeting_status NOT NULL DEFAULT 'planifiee',
    organizer_id  UUID NOT NULL,                          -- réf. logique people.staff.id
    formation_ref UUID,                                   -- réf. logique academic.formations.id
    version       BIGINT NOT NULL DEFAULT 0,              -- verrou optimiste
    created_by    UUID NOT NULL,                          -- auteur (identity.users.id)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT ck_reunions_dates CHECK (ends_at IS NULL OR ends_at >= starts_at)
);

CREATE INDEX idx_reunions_starts ON reunions (starts_at);
CREATE INDEX idx_reunions_type   ON reunions (type);

-- ------------------------------------------------------------
-- Table reunion_participants — participants (PK composite)
-- ------------------------------------------------------------
CREATE TABLE reunion_participants (
    reunion_id  UUID NOT NULL REFERENCES reunions (id) ON DELETE CASCADE,
    person_ref  UUID NOT NULL,                            -- staff ou étudiant (réf. logique)
    person_kind TEXT NOT NULL,
    is_present  BOOLEAN,                                  -- émargement
    PRIMARY KEY (reunion_id, person_ref),
    CONSTRAINT ck_participant_kind CHECK (person_kind IN ('staff', 'student'))
);

-- ------------------------------------------------------------
-- Table comptes_rendus — compte rendu
-- ------------------------------------------------------------
CREATE TABLE comptes_rendus (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reunion_id   UUID REFERENCES reunions (id) ON DELETE SET NULL,
    title        TEXT NOT NULL,
    type         meeting_type NOT NULL,
    body         TEXT,                                    -- contenu rédigé
    document_ref UUID,                                    -- réf. logique document.documents.id (PDF archivé)
    meeting_date DATE NOT NULL,
    author_id    UUID NOT NULL,                           -- réf. logique people.staff.id
    is_published BOOLEAN NOT NULL DEFAULT FALSE,          -- publication -> notifications
    published_at TIMESTAMPTZ,
    version      BIGINT NOT NULL DEFAULT 0,               -- verrou optimiste
    created_by   UUID NOT NULL,                           -- auteur (identity.users.id) = ownerId ABAC
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX idx_cr_reunion ON comptes_rendus (reunion_id);
CREATE INDEX idx_cr_date     ON comptes_rendus (meeting_date DESC);
-- Index partiel : n'indexe que les comptes rendus publiés.
CREATE INDEX idx_cr_pub ON comptes_rendus (id) WHERE is_published;

-- ------------------------------------------------------------
-- Table compte_rendu_visibility — visibilité basée rôle (ABAC anti-IDOR)
-- ------------------------------------------------------------
CREATE TABLE compte_rendu_visibility (
    compte_rendu_id UUID NOT NULL REFERENCES comptes_rendus (id) ON DELETE CASCADE,
    role            TEXT NOT NULL,
    PRIMARY KEY (compte_rendu_id, role)
);

-- ------------------------------------------------------------
-- Table notifications — push WebSocket
-- ------------------------------------------------------------
CREATE TABLE notifications (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id   UUID NOT NULL,                         -- réf. logique identity.users.id
    kind           notification_kind NOT NULL,
    title          TEXT NOT NULL,
    message        TEXT,
    target_service TEXT,                                  -- service cible (deep-link)
    target_ref     UUID,                                  -- ressource cible (deep-link)
    is_read        BOOLEAN NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMPTZ,
    delivered_ws   BOOLEAN NOT NULL DEFAULT FALSE,        -- poussée via WebSocket
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_recipient ON notifications (recipient_id, is_read, created_at DESC);

-- ------------------------------------------------------------
-- Read-models (projections Kafka, lecture seule, suffixe _ro)
-- Jamais écrits par l'API : alimentés uniquement par les consommateurs Kafka.
-- ------------------------------------------------------------

-- identity_user_ro : qui notifier, par rôle (consomme identity.users)
CREATE TABLE identity_user_ro (
    id            UUID PRIMARY KEY,                       -- = identity.users.id
    full_name     TEXT NOT NULL,
    email         CITEXT,
    roles         TEXT[] NOT NULL DEFAULT '{}',           -- rôles applicatifs
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    last_event_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_offset  BIGINT
);

-- Recherche par rôle (résolution des destinataires d'une notification).
CREATE INDEX idx_identity_user_ro_roles ON identity_user_ro USING GIN (roles);

-- people_staff_ro : afficher l'auteur / l'organisateur (consomme people.staff)
CREATE TABLE people_staff_ro (
    id            UUID PRIMARY KEY,                       -- = people.staff.id
    full_name     TEXT NOT NULL,
    kind          TEXT NOT NULL,
    last_event_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_offset  BIGINT
);

-- ------------------------------------------------------------
-- Idempotence Kafka : déduplication des événements consommés (sur eventId).
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- Transactional Outbox : atomicité écriture DB + publication Kafka.
-- Un relais lit les lignes non publiées et émet sur le topic correspondant.
-- ------------------------------------------------------------
CREATE TABLE outbox (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type TEXT NOT NULL,                         -- Reunion, CompteRendu, Notification
    aggregate_id  UUID NOT NULL,                          -- = clé de partition Kafka
    topic         TEXT NOT NULL,
    event_type    TEXT NOT NULL,                          -- ReunionPlanifiee, CompteRenduPublie...
    trace_id      TEXT,                                   -- corrélation propagée
    payload       JSONB NOT NULL,                         -- état de l'agrégat (JSON)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ                             -- NULL tant que non relayé vers Kafka
);

-- Le relais ne balaye que les lignes en attente de publication.
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
