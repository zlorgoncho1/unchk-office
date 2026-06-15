-- ============================================================
-- Seed de démonstration : registre du courrier + communiqués administratifs.
-- Données reproductibles (down -v puis up) pour illustrer les modules
-- « courrier arrivé/départ » et « notes de service / circulaires ».
-- created_by = utilisateur système de démonstration.
-- ============================================================

-- ------------------------------------------------------------
-- Courriers (arrivés / départs) avec différents statuts.
-- ------------------------------------------------------------
INSERT INTO mails (id, reference, direction, subject, correspondent, mail_date, status, created_by) VALUES
  ('b1a10000-0000-4000-a000-000000000001', 'CA-2026-001', 'arrive',
   'Convocation au Conseil d''Administration de l''UNCHK', 'Ministère de l''Enseignement supérieur',
   DATE '2026-05-12', 'traite', '00000000-0000-0000-0000-000000000001'),
  ('b1a10000-0000-4000-a000-000000000002', 'CA-2026-002', 'arrive',
   'Demande de partenariat académique', 'Université Gaston Berger',
   DATE '2026-05-28', 'en_traitement', '00000000-0000-0000-0000-000000000001'),
  ('b1a10000-0000-4000-a000-000000000003', 'CD-2026-014', 'depart',
   'Transmission du rapport d''activités 2025', 'Direction générale de l''Enseignement supérieur',
   DATE '2026-04-30', 'clos', '00000000-0000-0000-0000-000000000001'),
  ('b1a10000-0000-4000-a000-000000000004', 'CD-2026-021', 'depart',
   'Invitation au séminaire pédagogique national', 'Universités numériques partenaires',
   DATE '2026-06-02', 'recu', '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- Communiqués : notes de service & circulaires.
-- Certaines sont publiées (état initial), d'autres en brouillon.
-- ------------------------------------------------------------
INSERT INTO admin_communiques (id, kind, reference, title, body, issue_date, is_published, published_at, created_by) VALUES
  ('c0110000-0000-4000-a000-000000000001', 'circulaire', 'CIRC-2026-03',
   'Calendrier des examens de fin de semestre',
   'La présente circulaire fixe le calendrier et les modalités d''organisation des examens de fin de semestre pour l''ensemble des formations.',
   DATE '2026-05-20', TRUE, TIMESTAMPTZ '2026-05-20 09:00:00+00', '00000000-0000-0000-0000-000000000001'),
  ('c0110000-0000-4000-a000-000000000002', 'note_service', 'NS-2026-07',
   'Horaires d''ouverture des services administratifs',
   'Note de service relative aux nouveaux horaires d''ouverture des services administratifs durant la période des inscriptions.',
   DATE '2026-05-25', TRUE, TIMESTAMPTZ '2026-05-25 10:30:00+00', '00000000-0000-0000-0000-000000000001'),
  ('c0110000-0000-4000-a000-000000000003', 'note_service', 'NS-2026-08',
   'Préparation de la rentrée académique 2026-2027',
   'Note de service (brouillon) sur l''organisation de la prochaine rentrée académique.',
   DATE '2026-06-05', FALSE, NULL, '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Ciblage par rôle (destinataires des communiqués).
INSERT INTO communique_targets (communique_id, role) VALUES
  ('c0110000-0000-4000-a000-000000000001', 'admin'),
  ('c0110000-0000-4000-a000-000000000001', 'administratif'),
  ('c0110000-0000-4000-a000-000000000001', 'enseignant'),
  ('c0110000-0000-4000-a000-000000000001', 'etudiant'),
  ('c0110000-0000-4000-a000-000000000002', 'admin'),
  ('c0110000-0000-4000-a000-000000000002', 'administratif'),
  ('c0110000-0000-4000-a000-000000000002', 'enseignant'),
  ('c0110000-0000-4000-a000-000000000002', 'appui-insertion'),
  ('c0110000-0000-4000-a000-000000000003', 'admin'),
  ('c0110000-0000-4000-a000-000000000003', 'administratif')
ON CONFLICT DO NOTHING;
