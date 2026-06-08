-- ============================================================
-- people-service — migration additive
-- Module Etudiant (enonce) : champ « Autres formations » texte libre.
-- Permet de saisir des formations complementaires non rattachees au catalogue
-- academique (formation_ref reste la formation principale).
-- ============================================================

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS other_trainings TEXT NULL;  -- autres formations (texte libre)
