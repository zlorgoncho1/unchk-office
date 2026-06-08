-- ============================================================
-- Base « admin » — admin-service
-- Gestion administrative : courrier arrivé/départ, notes de service, circulaires,
-- gestion budgétaire (projet de budget, note d'orientation, budget réalisé).
-- Topic produit : admin.budget.
--
-- Conventions transverses (cf. docs/database.md) :
--   * Clés primaires UUID (gen_random_uuid) — anti-énumération / anti-IDOR.
--   * created_at / updated_at TIMESTAMPTZ ; deleted_at (suppression logique).
--   * version BIGINT (verrouillage optimiste) sur les agrégats modifiables.
--   * Read-models *_ro alimentés UNIQUEMENT par Kafka (event_offset / last_event_at).
-- ============================================================

-- Extensions communes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- emails insensibles à la casse

-- ------------------------------------------------------------
-- Types énumérés
-- ------------------------------------------------------------
CREATE TYPE mail_direction AS ENUM ('arrive', 'depart');
CREATE TYPE mail_status    AS ENUM ('recu', 'en_traitement', 'traite', 'archive', 'clos');
CREATE TYPE admin_doc_kind AS ENUM ('note_service', 'circulaire');
CREATE TYPE budget_status  AS ENUM ('projet', 'vote', 'en_execution', 'cloture');

-- ------------------------------------------------------------
-- Fonction de trigger : tient updated_at à jour.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Table mails — courrier (arrivé / départ)
-- ============================================================
CREATE TABLE mails (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference     VARCHAR(64) UNIQUE,
    direction     mail_direction NOT NULL,
    subject       TEXT NOT NULL,
    correspondent TEXT NOT NULL,
    mail_date     DATE NOT NULL,
    registered_at DATE NOT NULL DEFAULT CURRENT_DATE,
    status        mail_status NOT NULL DEFAULT 'recu',
    assigned_to   UUID,                       -- → people.staff.id (réf logique)
    document_ref  UUID,                       -- → document.documents.id (scan/PDF)
    notes         TEXT,
    version       BIGINT NOT NULL DEFAULT 0,
    created_by    UUID NOT NULL,              -- → identity.users.id
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_mails_direction ON mails (direction, mail_date DESC);
CREATE INDEX idx_mails_status    ON mails (status);

CREATE TRIGGER trg_mails_updated_at
    BEFORE UPDATE ON mails
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Table admin_communiques — notes de service & circulaires
-- ============================================================
CREATE TABLE admin_communiques (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind         admin_doc_kind NOT NULL,
    reference    VARCHAR(64) UNIQUE,
    title        TEXT NOT NULL,
    body         TEXT,
    document_ref UUID,                        -- → document.documents.id
    issue_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    version      BIGINT NOT NULL DEFAULT 0,
    created_by   UUID NOT NULL,               -- → identity.users.id
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX idx_communiques_kind ON admin_communiques (kind, issue_date DESC);

CREATE TRIGGER trg_communiques_updated_at
    BEFORE UPDATE ON admin_communiques
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Table communique_targets — ciblage par rôle (détermine les destinataires)
-- ============================================================
CREATE TABLE communique_targets (
    communique_id UUID NOT NULL REFERENCES admin_communiques (id) ON DELETE CASCADE,
    role          TEXT NOT NULL,
    PRIMARY KEY (communique_id, role)
);

CREATE INDEX idx_communique_targets_role ON communique_targets (role);

-- ============================================================
-- Table budgets — budget (projet de budget + budget réalisé)
-- La note d'orientation budgétaire est portée par la colonne orientation_note.
-- ============================================================
CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year     SMALLINT NOT NULL,
    label           TEXT NOT NULL,
    status          budget_status NOT NULL DEFAULT 'projet',
    orientation_note TEXT,                    -- Note d'orientation budgétaire associée à l'exercice
    total_planned   NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK (total_planned  >= 0),
    total_realized  NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK (total_realized >= 0),
    currency        CHAR(3) NOT NULL DEFAULT 'XOF',
    owner_id        UUID,                      -- → identity.users.id (propriétaire ABAC anti-IDOR)
    version         BIGINT NOT NULL DEFAULT 0,
    created_by      UUID NOT NULL,             -- → identity.users.id
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_budgets_year_label UNIQUE (fiscal_year, label)
);

CREATE INDEX idx_budgets_year ON budgets (fiscal_year);

CREATE TRIGGER trg_budgets_updated_at
    BEFORE UPDATE ON budgets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Table budget_lines — lignes budgétaires (prévu vs réalisé par poste)
-- ============================================================
CREATE TABLE budget_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id       UUID NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    category        TEXT NOT NULL,
    direction       TEXT NOT NULL CHECK (direction IN ('depense', 'recette')),
    planned_amount  NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK (planned_amount  >= 0),
    realized_amount NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK (realized_amount >= 0),
    label           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_budget_lines_budget ON budget_lines (budget_id);

CREATE TRIGGER trg_budget_lines_updated_at
    BEFORE UPDATE ON budget_lines
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Read-model people_staff_ro — projection Kafka du topic people.staff.
-- Lecture seule : alimentée UNIQUEMENT par le consommateur Kafka, jamais par l'API.
-- Sert à afficher l'agent en charge d'un courrier sans appel REST inter-service.
-- ============================================================
CREATE TABLE people_staff_ro (
    id            UUID PRIMARY KEY,           -- = people.staff.id
    full_name     TEXT NOT NULL,
    kind          TEXT NOT NULL,
    department    TEXT,
    last_event_at TIMESTAMPTZ NOT NULL,
    event_offset  BIGINT
);

-- ============================================================
-- Table processed_events — idempotence de la consommation Kafka.
-- Chaque consommateur déduplique sur eventId (cf. docs/architecture.md §8).
-- ============================================================
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
