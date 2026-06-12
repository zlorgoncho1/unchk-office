-- ============================================================
-- Données de DÉMONSTRATION — document-service.
-- Jeu cohérent inter-services : les UUID sont conservés tels quels,
-- donc les références croisées (formationRef, userRef, organisateurs,
-- destinataires de notifications…) restent valides d'un service à l'autre.
-- Idempotent : ON CONFLICT DO NOTHING (rejouable sans casse).
-- FK désactivées le temps du chargement (ordre d'insertion indifférent).
-- Données extraites de la base de démo de référence, puis versionnées.
-- ============================================================
SET session_replication_role = replica;

--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14


--
-- Data for Name: documents; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('1c88b429-f5fb-4ad3-85bf-b1c7df65ef9d', 'Courrier arrive 2026-014 - Convention de partenariat Sonatel', 'courrier', 'Courrier recu de Sonatel relatif a la convention de partenariat pour les stages des etudiants en Licence Reseaux & Telecoms.', 'courriers', '1c88b429-f5fb-4ad3-85bf-b1c7df65ef9d/courrier1.pdf', 'application/pdf', 476, '5bff998d770edc4c3500730ec1f685ddae7d0e85381acd7a76e190aa1e83e97d', '00000000-0000-0000-0000-000000000001', false, 'admin-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:57.927793+00', '2026-06-08 12:26:57.927793+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('9baa2649-994c-4427-9387-011ca88a8f1b', 'Courrier depart 2026-031 - Reponse a Orange Senegal (Hackathon)', 'courrier', 'Lettre adressee a Orange Senegal confirmant la participation des etudiants du Master Cybersecurite au hackathon national.', 'courriers', '9baa2649-994c-4427-9387-011ca88a8f1b/f2.pdf', 'application/pdf', 492, '25b57c1d4bc43cf3284038fdd911079fc46bbe423a662dc1a738f59d548c76bb', '00000000-0000-0000-0000-000000000001', false, 'admin-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:01.891608+00', '2026-06-08 12:28:01.891608+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('74f7f0ea-149b-4c37-a3a4-572acc5f96b0', 'Note de service N0152 - Calendrier de reprise des cours', 'note_service', 'Note informant les enseignants et etudiants de la reprise des cours en presentiel et en ligne pour le semestre 2.', 'documents', '74f7f0ea-149b-4c37-a3a4-572acc5f96b0/f3.pdf', 'application/pdf', 489, 'c798e0f5950ee6b09326e20fa3146719bc2e905299c3419b0f29a3a73141caaa', '00000000-0000-0000-0000-000000000001', false, 'academic-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:01.959895+00', '2026-06-08 12:28:01.959895+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'Circulaire 2026-07 - Modalites des examens de fin de semestre', 'circulaire', 'Circulaire precisant les modalites d organisation des examens (presentiel, surveillance, delais de depot des notes).', 'documents', 'a4da67a5-b7f4-43c1-a554-6743e3b47222/f4.pdf', 'application/pdf', 479, '9727a9b3fbfc10e97530375a370e83049c9d765a5b7ddcb05b4c97d9b52511f8', '00000000-0000-0000-0000-000000000001', false, 'academic-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:02.013675+00', '2026-06-08 12:28:02.013675+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('4b52d5c5-df01-4d91-8030-4240a29f25c6', 'Note de service N0158 - Planning des conges annuels 2026', 'note_service', 'Planning previsionnel des conges annuels du personnel administratif et technique de l UNCHK.', 'documents', '4b52d5c5-df01-4d91-8030-4240a29f25c6/f5.pdf', 'application/pdf', 480, 'fb8a4523b02ece1df396cb7bca11d73350148ab88955bf9da2ea3bc0104200ea', '00000000-0000-0000-0000-000000000001', false, 'admin-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:02.068574+00', '2026-06-08 12:28:02.068574+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('3a7b7aa9-de9f-4c22-9780-ce0701be9387', 'Courrier arrive 2026-040 - Proposition Wave (paiement frais)', 'courrier', 'Proposition de Wave pour l integration du paiement mobile des frais d inscription et de scolarite des etudiants.', 'courriers', '3a7b7aa9-de9f-4c22-9780-ce0701be9387/f6.pdf', 'application/pdf', 487, '1b53179723e0b643a2cc922021c7cff8659acd6a6a56abd4cd3549e7f6c8d2dc', '00000000-0000-0000-0000-000000000001', false, 'admin-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:02.12193+00', '2026-06-08 12:28:02.12193+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'Circulaire 2026-09 - Conventions de stage avec les partenaires', 'circulaire', 'Circulaire relative aux conventions de stage signees avec Sonatel, Atos Senegal et Senegal Numerique SA pour les etudiants de Master Genie Logiciel.', 'documents', '05b61efe-170a-4615-85c9-6a8985aa75a7/f7.pdf', 'application/pdf', 485, '28ca8ead7f598127b8741be8c848302b2833fa678620217649192c9630292041', '00000000-0000-0000-0000-000000000001', false, 'insertion-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:02.173751+00', '2026-06-08 12:28:02.173751+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.documents (id, title, category, description, bucket, object_key, mime_type, size_bytes, checksum_sha256, owner_id, is_archived, source_service, source_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('66e882f3-fbbd-4650-847b-7b527a4dff6d', 'Note de service N0161 - Maintenance de la plateforme ENT', 'note_service', 'Note informant d une interruption planifiee de l Environnement Numerique de Travail pour mise a jour de securite.', 'documents', '66e882f3-fbbd-4650-847b-7b527a4dff6d/f8.pdf', 'application/pdf', 470, '3667dc9e9f0fce3f65d3325d15be32af66abd656b8a44e095a94d95e64ec041b', '00000000-0000-0000-0000-000000000001', false, 'admin-service', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:02.225226+00', '2026-06-08 12:28:02.225226+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: document_visibility; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.document_visibility (document_id, role) VALUES ('1c88b429-f5fb-4ad3-85bf-b1c7df65ef9d', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('1c88b429-f5fb-4ad3-85bf-b1c7df65ef9d', 'agent_courrier') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('1c88b429-f5fb-4ad3-85bf-b1c7df65ef9d', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('9baa2649-994c-4427-9387-011ca88a8f1b', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('9baa2649-994c-4427-9387-011ca88a8f1b', 'agent_courrier') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('74f7f0ea-149b-4c37-a3a4-572acc5f96b0', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('74f7f0ea-149b-4c37-a3a4-572acc5f96b0', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('74f7f0ea-149b-4c37-a3a4-572acc5f96b0', 'etudiant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('74f7f0ea-149b-4c37-a3a4-572acc5f96b0', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'etudiant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('a4da67a5-b7f4-43c1-a554-6743e3b47222', 'scolarite') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('4b52d5c5-df01-4d91-8030-4240a29f25c6', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('4b52d5c5-df01-4d91-8030-4240a29f25c6', 'rh') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('4b52d5c5-df01-4d91-8030-4240a29f25c6', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('3a7b7aa9-de9f-4c22-9780-ce0701be9387', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('3a7b7aa9-de9f-4c22-9780-ce0701be9387', 'agent_courrier') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('3a7b7aa9-de9f-4c22-9780-ce0701be9387', 'finance') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('3a7b7aa9-de9f-4c22-9780-ce0701be9387', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'etudiant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'insertion') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('05b61efe-170a-4615-85c9-6a8985aa75a7', 'direction') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('66e882f3-fbbd-4650-847b-7b527a4dff6d', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('66e882f3-fbbd-4650-847b-7b527a4dff6d', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('66e882f3-fbbd-4650-847b-7b527a4dff6d', 'etudiant') ON CONFLICT DO NOTHING;
INSERT INTO public.document_visibility (document_id, role) VALUES ('66e882f3-fbbd-4650-847b-7b527a4dff6d', 'direction') ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
