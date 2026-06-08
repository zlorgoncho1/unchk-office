-- ============================================================
-- Ajout additif de la valeur « rencontre » au type énuméré meeting_type.
-- Couvre les rencontres ponctuelles (échange / point informel) demandées
-- par le cahier des charges en plus des réunions, séminaires, webinaires...
--
-- NOTE : ALTER TYPE ... ADD VALUE ne peut pas s'exécuter dans une transaction
-- sur certaines versions de PostgreSQL ; on désactive donc la transaction Flyway
-- pour cette migration. IF NOT EXISTS rend l'opération idempotente.
-- ============================================================
-- flyway:executeInTransaction=false

ALTER TYPE meeting_type ADD VALUE IF NOT EXISTS 'rencontre';
