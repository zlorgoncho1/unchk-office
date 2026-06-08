-- ============================================================
-- people-service — base PostgreSQL "people"
-- Entites CANONIQUES Etudiant et Personnel/Formateur (refs UUID partout ailleurs).
-- DDL derive de docs/database.md (section 2. Base `people`).
-- ============================================================

-- Extensions communes (cf. conventions transverses docs/database.md).
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- emails insensibles a la casse

-- ------------------------------------------------------------
-- Types enumeres
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'genre') THEN
        CREATE TYPE genre AS ENUM ('homme', 'femme', 'autre');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'staff_kind') THEN
        CREATE TYPE staff_kind AS ENUM (
            'enseignant',
            'enseignant_associe',
            'responsable_formation',
            'tuteur',
            'administratif',
            'appui_insertion'
        );
    END IF;
END$$;

-- ------------------------------------------------------------
-- Fonction de trigger : met a jour updated_at a chaque modification
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Table students — ETUDIANT (canonique)
-- ============================================================
CREATE TABLE students (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ine              VARCHAR(32)  NOT NULL UNIQUE,
    matricule        VARCHAR(32)  UNIQUE,
    first_name       TEXT         NOT NULL,
    last_name        TEXT         NOT NULL,
    gender           genre        NOT NULL,
    birth_date       DATE         NULL,
    birth_place      TEXT         NULL,
    email            CITEXT       NULL,
    phone            VARCHAR(32)  NULL,
    address          TEXT         NULL,
    photo_object_key TEXT         NULL,            -- reference MinIO (bucket avatars)
    formation_ref    UUID         NULL,            -- -> academic.formations.id (ref logique)
    promotion        VARCHAR(32)  NULL,            -- ex "2023-2024"
    enrollment_year  SMALLINT     NULL,            -- annee de debut
    exit_year        SMALLINT     NULL,            -- annee de sortie
    user_ref         UUID         NULL,            -- -> identity.users.id (compte etudiant, ABAC me)
    status           TEXT         NOT NULL DEFAULT 'inscrit'
                                  CHECK (status IN ('inscrit', 'diplome', 'abandon', 'suspendu')),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_by       UUID         NULL,            -- -> identity.users.id (auteur)
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ  NULL
);

CREATE INDEX idx_students_name      ON students (last_name, first_name);
CREATE INDEX idx_students_formation ON students (formation_ref);
CREATE INDEX idx_students_promo     ON students (promotion);
-- Resolution rapide de la fiche "me" (anti-IDOR) a partir du compte utilisateur.
CREATE INDEX idx_students_user_ref  ON students (user_ref) WHERE user_ref IS NOT NULL;

CREATE TRIGGER trg_students_updated_at
    BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Table student_diplomas — diplomes obtenus
-- ============================================================
CREATE TABLE student_diplomas (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    label       TEXT        NOT NULL,
    level       TEXT        NULL,
    obtained_at DATE        NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_diplomas_student ON student_diplomas (student_id);

-- ============================================================
-- Table staff — PERSONNEL / FORMATEUR (canonique)
-- ============================================================
CREATE TABLE staff (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    matricule        VARCHAR(32)  UNIQUE,
    first_name       TEXT         NOT NULL,
    last_name        TEXT         NOT NULL,
    gender           genre        NOT NULL,
    kind             staff_kind   NOT NULL,
    email            CITEXT       NULL,
    phone            VARCHAR(32)  NULL,
    grade            TEXT         NULL,
    speciality       TEXT         NULL,            -- specialite (formateur)
    department       TEXT         NULL,
    photo_object_key TEXT         NULL,            -- reference MinIO
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    hired_at         DATE         NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_by       UUID         NULL,            -- -> identity.users.id (auteur)
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ  NULL
);

CREATE INDEX idx_staff_name ON staff (last_name, first_name);
CREATE INDEX idx_staff_kind ON staff (kind) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_staff_updated_at
    BEFORE UPDATE ON staff
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Read-model identity_user_ro (projection Kafka du topic identity.users)
-- people consomme identity.users (cf. matrice docs/architecture.md) afin de :
--  - relier un etudiant a son compte (user_ref) pour l'ABAC "fiche me" ;
--  - afficher l'auteur (created_by) sans appel REST.
-- Table en lecture seule : alimentee UNIQUEMENT par le consommateur Kafka.
-- ============================================================
CREATE TABLE identity_user_ro (
    id            UUID        PRIMARY KEY,         -- = identity.users.id
    full_name     TEXT        NOT NULL,
    email         CITEXT      NULL,
    roles         TEXT[]      NOT NULL DEFAULT '{}',
    person_ref    UUID        NULL,                -- -> people.students.id | people.staff.id
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    last_event_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_offset  BIGINT      NULL                 -- idempotence Kafka
);

CREATE INDEX idx_identity_user_ro_person ON identity_user_ro (person_ref) WHERE person_ref IS NOT NULL;

-- ============================================================
-- Idempotence des consommateurs Kafka : un event deja traite est ignore.
-- (cf. docs/architecture.md, table processed_events).
-- ============================================================
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
