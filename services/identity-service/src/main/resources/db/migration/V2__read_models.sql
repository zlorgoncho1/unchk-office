-- ============================================================
-- Read-models locaux et idempotence des consommateurs Kafka.
-- Conforme à l'architecture CQRS : identity-service maintient une projection
-- en lecture seule des personnes canoniques (people.students / people.staff)
-- pour valider/enrichir le lien person_ref des comptes, SANS aucun appel REST.
-- ============================================================

-- ------------------------------------------------------------
-- Table processed_events — idempotence des consommateurs.
-- Chaque eventId Kafka traité est mémorisé pour éviter le double-traitement (rejeu).
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- Read-model read_person — projection locale des personnes canoniques.
-- Alimentée par les topics people.students et people.staff (lecture seule).
-- Sert à vérifier l'existence d'un person_ref lors de la création d'un compte.
-- ------------------------------------------------------------
CREATE TABLE read_person (
    id          UUID        PRIMARY KEY,
    person_kind TEXT        NOT NULL CHECK (person_kind IN ('etudiant', 'personnel')),
    full_name   TEXT        NULL,
    email       TEXT        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_read_person_kind ON read_person (person_kind);
