-- ============================================================
-- Base `academic` — academic-service
-- Formations (dates, type, niveau, financement, formés par genre),
-- emplois du temps (créneaux), affectation des formateurs.
-- Topic émis : academic.formations.
-- Conforme au DDL de docs/database.md (section 5).
-- ============================================================

-- Extensions communes (cf. conventions transverses docs/database.md).
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- chaînes insensibles à la casse

-- ------------------------------------------------------------
-- Énumérations
-- Conformément aux conventions (docs/database.md : « alternative CHECK quand l'énum
-- doit rester lisible/contrainte »), on modélise les énumérations comme des colonnes
-- TEXT contraintes par CHECK. Cela garde les libellés lisibles, évite les conflits avec
-- les mots réservés Java (ex : « continue ») et reste simple à faire évoluer.
-- Les valeurs autorisées correspondent EXACTEMENT au DDL de référence.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- Table `formations`
-- ------------------------------------------------------------
CREATE TABLE formations (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(32)     UNIQUE,
    label           TEXT            NOT NULL,
    level           TEXT            NOT NULL
        CHECK (level IN ('certificat', 'licence', 'master', 'doctorat', 'formation_continue')),
    kind            TEXT            NOT NULL DEFAULT 'initiale'
        CHECK (kind IN ('initiale', 'continue', 'professionnelle', 'diplomante', 'qualifiante')),
    funding         TEXT            NULL
        CHECK (funding IN ('etat', 'partenaire', 'autofinancement', 'projet', 'mixte')),
    start_date      DATE            NULL,
    end_date        DATE            NULL,
    trained_male    INT             NOT NULL DEFAULT 0 CHECK (trained_male >= 0),
    trained_female  INT             NOT NULL DEFAULT 0 CHECK (trained_female >= 0),
    responsible_ref UUID            NULL,   -- -> people.staff.id (réf logique, hors base)
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_by      UUID            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ     NULL,
    CONSTRAINT chk_formations_periode
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_formations_level ON formations (level);

-- ------------------------------------------------------------
-- Table `formation_formateurs` — affectation des formateurs
-- PK composite (formation_id, formateur_ref, module).
-- ------------------------------------------------------------
CREATE TABLE formation_formateurs (
    formation_id  UUID         NOT NULL REFERENCES formations (id) ON DELETE CASCADE,
    formateur_ref UUID         NOT NULL,   -- -> people.staff.id (réf logique, hors base)
    module        TEXT         NOT NULL,   -- matière enseignée
    assigned_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_formation_formateurs PRIMARY KEY (formation_id, formateur_ref, module)
);

-- ------------------------------------------------------------
-- Table `schedule_slots` — emploi du temps (créneaux)
-- ------------------------------------------------------------
CREATE TABLE schedule_slots (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    formation_id  UUID         NOT NULL REFERENCES formations (id) ON DELETE CASCADE,
    course_label  TEXT         NOT NULL,
    formateur_ref UUID         NULL,   -- -> people.staff.id (réf logique, hors base)
    day_of_week   TEXT         NULL    -- récurrent hebdo
        CHECK (day_of_week IN ('lundi','mardi','mercredi','jeudi','vendredi','samedi','dimanche')),
    session_date  DATE         NULL,   -- ou date ponctuelle
    start_time    TIME         NOT NULL,
    end_time      TIME         NOT NULL,
    room          TEXT         NULL,   -- salle ou lien visio
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_slots_horaires CHECK (end_time > start_time),
    CONSTRAINT chk_slots_recurrence CHECK (day_of_week IS NOT NULL OR session_date IS NOT NULL)
);

CREATE INDEX idx_slots_formation ON schedule_slots (formation_id);
CREATE INDEX idx_slots_formateur ON schedule_slots (formateur_ref);
CREATE INDEX idx_slots_date      ON schedule_slots (session_date);

-- ------------------------------------------------------------
-- Read-model (projection Kafka) : academic_formateur_ro
-- Alimenté en consommant people.staff (JAMAIS d'appel REST).
-- Permet d'afficher les noms des formateurs localement.
-- ------------------------------------------------------------
CREATE TABLE academic_formateur_ro (
    id            UUID         PRIMARY KEY,   -- = people.staff.id
    full_name     TEXT         NOT NULL,
    kind          TEXT         NOT NULL,      -- enseignant, tuteur...
    speciality    TEXT         NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_event_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    event_offset  BIGINT       NULL           -- idempotence Kafka
);

CREATE INDEX idx_academic_formateur_ro_kind ON academic_formateur_ro (kind);

-- ------------------------------------------------------------
-- Idempotence des consommateurs Kafka (déduplication sur eventId).
-- Table transverse recommandée par l'architecture event-driven.
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
