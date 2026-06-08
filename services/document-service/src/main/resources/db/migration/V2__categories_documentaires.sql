-- ============================================================
-- document-service — V2 : enrichissement des catégories documentaires
-- L'énoncé exige des catégories précises (courrier arrivé / départ,
-- notes de service interne / externe, note administrative).
-- Migration ADDITIVE : on ne modifie pas V1, on étend le domaine de valeurs.
-- Les anciennes catégories sont conservées pour compatibilité.
-- ============================================================

-- ------------------------------------------------------------
-- 1) Type énuméré PostgreSQL document_category : ajout des nouvelles valeurs.
--    Conservé en cohérence avec V1 (même si la colonne s'appuie sur un CHECK).
--    ADD VALUE IF NOT EXISTS est idempotent et ne casse pas une relance.
-- ------------------------------------------------------------
ALTER TYPE document_category ADD VALUE IF NOT EXISTS 'courrier_arrive';
ALTER TYPE document_category ADD VALUE IF NOT EXISTS 'courrier_depart';
ALTER TYPE document_category ADD VALUE IF NOT EXISTS 'note_service_interne';
ALTER TYPE document_category ADD VALUE IF NOT EXISTS 'note_service_externe';
ALTER TYPE document_category ADD VALUE IF NOT EXISTS 'note_administrative';

-- ------------------------------------------------------------
-- 2) Contrainte CHECK de la colonne documents.category : on recrée la
--    contrainte (modélisation réelle = VARCHAR + CHECK, cf. V1) pour
--    autoriser les nouveaux codes tout en gardant les anciens.
-- ------------------------------------------------------------
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_category_check;
ALTER TABLE documents
    ADD CONSTRAINT documents_category_check
    CHECK (category IN (
        'logo', 'compte_rendu',
        'courrier', 'courrier_arrive', 'courrier_depart',
        'note_service', 'note_service_interne', 'note_service_externe',
        'note_administrative', 'circulaire', 'rapport', 'autre'
    ));
