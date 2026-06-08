-- ============================================================
-- Base `identity` — identity-service
-- Source de vérité des comptes utilisateurs et des rôles (identité fédérée maison).
-- DDL repris de docs/database.md (section 1). Clés primaires en UUID (anti-IDOR).
-- ============================================================

-- Extensions nécessaires : gen_random_uuid() (pgcrypto) et CITEXT (email insensible à la casse).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- ------------------------------------------------------------
-- Type énuméré des rôles applicatifs (5 rôles).
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role_code') THEN
        CREATE TYPE role_code AS ENUM (
            'admin', 'administratif', 'enseignant', 'appui-insertion', 'etudiant'
        );
    END IF;
END$$;

-- ------------------------------------------------------------
-- Table users — comptes utilisateurs (authentification).
-- Le hash du mot de passe n'est JAMAIS exposé ni publié sur Kafka.
-- ------------------------------------------------------------
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           CITEXT      NOT NULL UNIQUE,
    password_hash   TEXT        NOT NULL,
    full_name       TEXT        NOT NULL,
    person_ref      UUID        NULL,
    person_kind     TEXT        NULL CHECK (person_kind IN ('etudiant', 'personnel')),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    is_locked       BOOLEAN     NOT NULL DEFAULT FALSE,
    failed_attempts INT         NOT NULL DEFAULT 0,
    last_login_at   TIMESTAMPTZ NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ NULL
);

-- Index partiels demandés par la documentation.
CREATE INDEX idx_users_person_ref ON users (person_ref) WHERE person_ref IS NOT NULL;
CREATE INDEX idx_users_active      ON users (id)         WHERE deleted_at IS NULL;

-- ------------------------------------------------------------
-- Table user_roles — affectation des rôles (cumul possible).
-- ------------------------------------------------------------
CREATE TABLE user_roles (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       role_code   NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by UUID        NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_role ON user_roles (role);

-- ------------------------------------------------------------
-- Table signing_keys — clés de signature JWT (rotation, exposées via JWKS).
-- public_pem est exposé par /.well-known/jwks.json ; private_pem reste secret.
-- ------------------------------------------------------------
CREATE TABLE signing_keys (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    kid         TEXT        NOT NULL UNIQUE,
    algorithm   TEXT        NOT NULL DEFAULT 'RS256',
    public_pem  TEXT        NOT NULL,
    private_pem TEXT        NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    rotated_at  TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Garantit qu'une seule clé est active à la fois (utilisée pour signer).
CREATE UNIQUE INDEX uq_signing_keys_active ON signing_keys (is_active) WHERE is_active;

-- ------------------------------------------------------------
-- Table refresh_tokens — révocation / anti-rejeu.
-- On ne stocke que le hash du token, jamais le token brut.
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

-- ------------------------------------------------------------
-- Table auth_audit — journal d'authentification (OWASP : traçabilité A09).
-- ------------------------------------------------------------
CREATE TABLE auth_audit (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NULL,
    event       TEXT        NOT NULL,
    ip_address  INET        NULL,
    user_agent  TEXT        NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_audit_user ON auth_audit (user_id, occurred_at DESC);
