-- ============================================================
-- Base `academic` — academic-service
-- Ajout du MONTANT de financement aux formations.
-- L'énoncé demande « Montant ET type de financement » : le type (funding)
-- existait déjà, on complète avec le montant.
-- Migration ADDITIVE : on ajoute simplement une colonne nullable.
-- ============================================================

-- Montant du financement (devise locale), optionnel et positif ou nul.
ALTER TABLE formations
    ADD COLUMN IF NOT EXISTS amount NUMERIC(14, 2) NULL
        CHECK (amount IS NULL OR amount >= 0);
