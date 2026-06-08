-- ============================================================
-- Base « insertion » — insertion-service
-- Appui à l'insertion : partenaires, stages, registre de contact,
-- statistiques d'insertion (auto-emploi vs emploi salarié).
-- Schéma issu de docs/database.md (section 6). Clés primaires UUID (anti-IDOR).
-- ============================================================

-- Extensions communes (UUID + emails insensibles à la casse).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- ------------------------------------------------------------
-- Types énumérés du domaine insertion.
-- ------------------------------------------------------------
CREATE TYPE partner_kind AS ENUM ('entreprise', 'administration', 'ong', 'institution', 'autre');
CREATE TYPE internship_status AS ENUM ('prevu', 'en_cours', 'termine', 'rompu', 'valide');
CREATE TYPE insertion_kind AS ENUM ('emploi_salarie', 'auto_emploi', 'recherche_emploi', 'poursuite_etudes', 'sans_activite');

-- ------------------------------------------------------------
-- Table partners — partenaires (structures d'accueil).
-- ------------------------------------------------------------
CREATE TABLE partners (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL,
    kind          partner_kind NOT NULL DEFAULT 'entreprise',
    sector        TEXT,
    contact_name  TEXT,
    contact_email CITEXT,
    contact_phone VARCHAR(32),
    address       TEXT,
    city          TEXT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    version       BIGINT NOT NULL DEFAULT 0,
    created_by    UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_partners_name_city UNIQUE (name, city)
);
CREATE INDEX idx_partners_kind ON partners (kind);

-- ------------------------------------------------------------
-- Table internships — stages (bilan de stages).
-- ------------------------------------------------------------
CREATE TABLE internships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_ref     UUID NOT NULL,                      -- → people.students.id (réf logique)
    partner_id      UUID REFERENCES partners (id) ON DELETE SET NULL,
    title           TEXT NOT NULL,
    start_date      DATE,
    end_date        DATE,
    status          internship_status NOT NULL DEFAULT 'prevu',
    tutor_ref       UUID,                               -- → people.staff.id (tuteur)
    supervisor_name TEXT,                               -- maître de stage côté partenaire
    report_ref      UUID,                               -- → document.documents.id (rapport)
    grade           NUMERIC(4,2),
    version         BIGINT NOT NULL DEFAULT 0,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT ck_internships_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_internships_student ON internships (student_ref);
CREATE INDEX idx_internships_partner ON internships (partner_id);
CREATE INDEX idx_internships_status  ON internships (status);

-- ------------------------------------------------------------
-- Table contact_log — registre de contact (devenir des diplômés).
-- ------------------------------------------------------------
CREATE TABLE contact_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_ref  UUID NOT NULL,                         -- → people.students.id
    contacted_at DATE NOT NULL DEFAULT CURRENT_DATE,
    channel      TEXT,                                  -- téléphone, email, présentiel
    notes        TEXT,
    agent_ref    UUID,                                  -- → people.staff.id (appui-insertion)
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_contact_student ON contact_log (student_ref);

-- ------------------------------------------------------------
-- Table insertion_outcomes — situation d'insertion (stats).
-- ------------------------------------------------------------
CREATE TABLE insertion_outcomes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_ref   UUID NOT NULL,                        -- → people.students.id
    formation_ref UUID,                                 -- → academic.formations.id (pour stats)
    kind          insertion_kind NOT NULL,
    employer_name TEXT,
    job_title     TEXT,
    observed_at   DATE NOT NULL DEFAULT CURRENT_DATE,
    is_current    BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outcomes_student   ON insertion_outcomes (student_ref);
CREATE INDEX idx_outcomes_kind      ON insertion_outcomes (kind);
CREATE INDEX idx_outcomes_formation ON insertion_outcomes (formation_ref) WHERE is_current;

-- ------------------------------------------------------------
-- Read-models (projections Kafka) — alimentés par les consommateurs,
-- JAMAIS écrits par l'API. Servent aux statistiques sans appel REST.
-- ------------------------------------------------------------

-- Projection de people.students (genre + formation → stats).
CREATE TABLE people_student_ro (
    id            UUID PRIMARY KEY,                     -- = people.students.id
    full_name     TEXT NOT NULL,
    gender        TEXT NOT NULL,
    formation_ref UUID,
    promotion     VARCHAR(32),
    exit_year     SMALLINT,
    last_event_at TIMESTAMPTZ NOT NULL,
    event_offset  BIGINT
);
CREATE INDEX idx_people_student_ro_formation ON people_student_ro (formation_ref);

-- Projection de academic.formations (libellé + niveau → stats).
CREATE TABLE academic_formation_ro (
    id            UUID PRIMARY KEY,                     -- = academic.formations.id
    label         TEXT NOT NULL,
    level         TEXT NOT NULL,
    last_event_at TIMESTAMPTZ NOT NULL,
    event_offset  BIGINT
);

-- ------------------------------------------------------------
-- Idempotence Kafka : journal des événements déjà traités par les consommateurs.
-- (dédoublonnage sur eventId — cf. enveloppe DomainEvent).
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    topic        TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
