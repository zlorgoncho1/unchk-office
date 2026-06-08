-- ============================================================
-- document-service — schéma de la base "document"
-- Métadonnées documentaires ; le binaire est stocké dans MinIO.
-- Source : docs/database.md (section 3 — base "document").
-- ============================================================

-- Extension pour la génération d'UUID côté base (gen_random_uuid).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ------------------------------------------------------------
-- Type énuméré : catégorie de document
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'document_category') THEN
        CREATE TYPE document_category AS ENUM (
            'logo', 'compte_rendu', 'courrier', 'note_service',
            'circulaire', 'rapport', 'autre'
        );
    END IF;
END$$;

-- ------------------------------------------------------------
-- Table documents — métadonnées (binaire dans MinIO)
-- ------------------------------------------------------------
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           TEXT NOT NULL,
    -- Mappée en VARCHAR côté JPA ; CHECK garantit les valeurs de l'énuméré métier.
    category        VARCHAR(32) NOT NULL DEFAULT 'autre'
                        CHECK (category IN ('logo', 'compte_rendu', 'courrier',
                                            'note_service', 'circulaire', 'rapport', 'autre')),
    description     TEXT,
    bucket          TEXT NOT NULL,
    object_key      TEXT NOT NULL,
    mime_type       TEXT NOT NULL,
    size_bytes      BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256 CHAR(64),
    owner_id        UUID NOT NULL,
    is_archived     BOOLEAN NOT NULL DEFAULT FALSE,
    source_service  TEXT,
    source_ref      UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    -- Le couple (bucket, clé objet) identifie de façon unique le binaire MinIO.
    CONSTRAINT uq_documents_bucket_object UNIQUE (bucket, object_key)
);

-- Index partiel : on n'indexe la catégorie que pour les documents non supprimés.
CREATE INDEX idx_documents_category ON documents (category) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_owner ON documents (owner_id);
CREATE INDEX idx_documents_source ON documents (source_service, source_ref);

-- ------------------------------------------------------------
-- Table document_visibility — visibilité basée rôle (ABAC -> OPA)
-- Alimente le visibility[] exposé à OPA pour l'anti-IDOR.
-- ------------------------------------------------------------
CREATE TABLE document_visibility (
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    role        TEXT NOT NULL,
    PRIMARY KEY (document_id, role)
);

CREATE INDEX idx_doc_visibility_role ON document_visibility (role);

-- ------------------------------------------------------------
-- Table document_shares — partage nominatif (par utilisateur)
-- ------------------------------------------------------------
CREATE TABLE document_shares (
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    can_edit    BOOLEAN NOT NULL DEFAULT FALSE,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (document_id, user_id)
);

-- ------------------------------------------------------------
-- Read-model local : projection des comptes utilisateurs (identity.users)
-- Alimenté par Kafka. Aucun appel REST inter-service.
-- ------------------------------------------------------------
CREATE TABLE identity_user_ro (
    id         UUID PRIMARY KEY,
    -- Rôles applicatifs de l'utilisateur (séparés par des virgules).
    roles      TEXT,
    status     TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- Outbox transactionnel : atomicité écriture base + publication Kafka.
-- Un relais lit cette table et publie sur le topic document.documents.
-- ------------------------------------------------------------
CREATE TABLE outbox (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type TEXT NOT NULL,
    aggregate_id   UUID NOT NULL,
    topic          TEXT NOT NULL,
    event_type     TEXT NOT NULL,
    event_version  INT NOT NULL DEFAULT 1,
    -- Charge utile JSON (état de l'agrégat) prête à émettre.
    payload        JSONB NOT NULL,
    trace_id       TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- Index sur les messages non encore publiés (file de sortie du relais).
CREATE INDEX idx_outbox_non_publie ON outbox (created_at) WHERE published_at IS NULL;

-- ------------------------------------------------------------
-- Idempotence consommateur : déduplication sur eventId.
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
