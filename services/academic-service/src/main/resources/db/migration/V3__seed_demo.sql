-- ============================================================
-- Données de DÉMONSTRATION — academic-service.
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
-- Data for Name: academic_formateur_ro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('0caafe65-9f7a-488c-9c52-e6620b803d6e', 'Khady Faye', 'enseignant', 'Developpement Web', true, '2026-06-08 12:27:07.948486+00', 0) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('fbec7acb-d810-4327-b46a-876d8e6fee0d', 'Moussa Cisse', 'enseignant', 'Cybersecurite', true, '2026-06-08 12:27:07.971573+00', 0) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('44e9da56-509f-4579-9c0a-1ef8181ab839', 'Seydou Ba', 'enseignant_associe', 'Reseaux & Telecoms', true, '2026-06-08 12:27:07.976822+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('442f2765-25fb-4cda-a1d7-ecdce1c2bdec', 'Cheikh Gueye', 'enseignant', 'Multimedia', false, '2026-06-08 12:27:07.979864+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('cfa0fa36-c3d9-433e-9b11-1e97ddc23485', 'Mariama Diop', 'administratif', NULL, true, '2026-06-08 12:27:07.982528+00', 0) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('77b0fc11-5b06-41b7-afbf-dc14c06eead6', 'Aminata Niang', 'responsable_formation', 'Genie Logiciel', true, '2026-06-08 12:27:07.986601+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('cf3b3942-919b-4c04-8b6c-ed23bcf92f9e', 'Ibrahima Thiam', 'tuteur', 'Data Science', true, '2026-06-08 12:27:07.989778+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('a0e0537d-97cf-4354-84e2-5cd72ded36b0', 'Fatou Sow', 'appui_insertion', NULL, true, '2026-06-08 12:27:07.993005+00', 3) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formateur_ro (id, full_name, kind, speciality, is_active, last_event_at, event_offset) VALUES ('e300536d-7ce5-4054-8bcb-0f1595aee107', 'TESTQA0 TESTQA1', 'enseignant', 'TESTQA4', true, '2026-06-12 01:42:50.329716+00', 1) ON CONFLICT DO NOTHING;


--
-- Data for Name: formations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('9d333483-dfe3-48dd-ae76-80999934de8e', 'LIC-DEV-WEB', 'Licence Développement Web', 'licence', 'initiale', 'etat', '2024-10-01', '2027-06-30', 62, 48, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.047322+00', '2026-06-08 12:26:43.047322+00', NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('87424228-2331-46a5-b31b-583c21722562', 'MAS-GEN-LOG', 'Master Génie Logiciel', 'master', 'professionnelle', 'partenaire', '2024-11-04', '2026-09-30', 34, 21, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.234179+00', '2026-06-08 12:26:43.234179+00', NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('e9313d0a-b73d-4160-ac4e-d5d119b61a35', 'LIC-RES-TEL', 'Licence Réseaux & Télécoms', 'licence', 'initiale', 'projet', '2025-01-13', '2028-06-30', 55, 29, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.274755+00', '2026-06-08 12:26:43.274755+00', NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'MAS-DATA-SCI', 'Master Data Science', 'master', 'diplomante', 'mixte', '2025-02-03', '2027-01-31', 40, 38, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.313859+00', '2026-06-08 12:26:43.313859+00', NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('8def8e75-ccb6-432c-be54-0a9d233855da', 'CERT-CYBER', 'Certificat Cybersécurité', 'certificat', 'qualifiante', 'autofinancement', '2025-09-15', '2026-03-15', 18, 12, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.350163+00', '2026-06-08 12:26:43.350163+00', NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.formations (id, code, label, level, kind, funding, start_date, end_date, trained_male, trained_female, responsible_ref, is_active, version, created_by, created_at, updated_at, deleted_at, amount) VALUES ('3f33149d-1fe9-424e-b4ac-f0b2301ed6b0', 'FC-MULTIMEDIA', 'Formation Continue Multimédia', 'formation_continue', 'continue', 'partenaire', '2026-01-12', '2026-07-10', 15, 25, NULL, true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:43.380454+00', '2026-06-08 12:26:43.380454+00', NULL, NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: schedule_slots; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('9a9553fe-d3d7-49d2-8b58-fd622f6a491b', '9d333483-dfe3-48dd-ae76-80999934de8e', 'Algorithmique et structures de données', NULL, 'lundi', NULL, '08:00:00', '10:00:00', 'Salle A1', '2026-06-08 12:27:56.847678+00', '2026-06-08 12:27:56.847678+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('f2e0d8f0-722e-4741-adc4-fcf981ef6ebd', '9d333483-dfe3-48dd-ae76-80999934de8e', 'Programmation Java', NULL, 'mardi', NULL, '10:15:00', '12:15:00', 'Salle B2', '2026-06-08 12:27:56.899882+00', '2026-06-08 12:27:56.899882+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('9e68f636-6eed-4716-8945-d605804d5545', '9d333483-dfe3-48dd-ae76-80999934de8e', 'Bases de données relationnelles', NULL, 'jeudi', NULL, '14:00:00', '16:00:00', 'Visio Teams', '2026-06-08 12:27:56.928827+00', '2026-06-08 12:27:56.928827+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('c31fc135-01f4-4682-9518-ca8342911bbf', '87424228-2331-46a5-b31b-583c21722562', 'Architecture microservices', NULL, 'mercredi', NULL, '09:00:00', '12:00:00', 'Salle C3', '2026-06-08 12:27:56.956801+00', '2026-06-08 12:27:56.956801+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('3a998594-8d2d-4263-8cf9-b562363f93c6', '87424228-2331-46a5-b31b-583c21722562', 'DevOps et CI/CD', NULL, 'vendredi', NULL, '14:30:00', '17:30:00', 'Labo Informatique', '2026-06-08 12:27:56.978561+00', '2026-06-08 12:27:56.978561+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('34968784-45fa-4f5f-9e1d-2ddf31efc588', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'Apprentissage automatique', NULL, 'lundi', NULL, '10:00:00', '13:00:00', 'Salle D4', '2026-06-08 12:27:57.000665+00', '2026-06-08 12:27:57.000665+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('ad59ddaa-765d-46da-8aef-813875ac2344', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'Big Data et Spark', NULL, 'mardi', NULL, '08:30:00', '11:30:00', 'Visio Zoom', '2026-06-08 12:27:57.028594+00', '2026-06-08 12:27:57.028594+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('9251d589-5cc0-4b93-aab6-817d04755241', 'e9313d0a-b73d-4160-ac4e-d5d119b61a35', 'Réseaux TCP/IP', NULL, 'jeudi', NULL, '08:00:00', '10:00:00', 'Salle E5', '2026-06-08 12:27:57.067067+00', '2026-06-08 12:27:57.067067+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('93204c3e-e8b9-4987-841b-23ce3c9cd67f', '8def8e75-ccb6-432c-be54-0a9d233855da', 'Tests d intrusion (atelier)', NULL, NULL, '2025-10-20', '09:00:00', '17:00:00', 'Labo Sécurité', '2026-06-08 12:27:57.093616+00', '2026-06-08 12:27:57.093616+00') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_slots (id, formation_id, course_label, formateur_ref, day_of_week, session_date, start_time, end_time, room, created_at, updated_at) VALUES ('d575fc4a-c74d-4c2d-b510-05f2a6a78d14', '8def8e75-ccb6-432c-be54-0a9d233855da', 'Cryptographie appliquée', NULL, 'samedi', NULL, '09:00:00', '12:00:00', 'Salle F6', '2026-06-08 12:27:57.117084+00', '2026-06-08 12:27:57.117084+00') ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
