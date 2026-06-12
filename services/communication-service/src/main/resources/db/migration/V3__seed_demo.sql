-- ============================================================
-- Données de DÉMONSTRATION — communication-service.
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
-- Data for Name: reunions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.reunions (id, title, type, description, location, starts_at, ends_at, status, organizer_id, formation_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('216238cf-fd96-42be-b768-18bf103100eb', 'Conseil d Universite - Session ordinaire Juin 2026', 'conseil_universite', 'Examen du budget previsionnel et des nouvelles formations en ligne.', 'Salle du Conseil - Siege UNCHK Dakar', '2026-06-15 09:00:00+00', '2026-06-15 12:30:00+00', 'planifiee', 'cfa0fa36-c3d9-433e-9b11-1e97ddc23485', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:27.491856+00', '2026-06-08 12:27:27.491856+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.reunions (id, title, type, description, location, starts_at, ends_at, status, organizer_id, formation_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('27690a64-5420-4736-a325-64f9d46bd6ae', 'Seminaire pedagogique - Innovation numerique', 'seminaire', 'Atelier sur les methodes d enseignement a distance et les outils collaboratifs.', 'Amphi virtuel - Visioconference', '2026-05-20 14:00:00+00', '2026-05-20 17:00:00+00', 'planifiee', '44e9da56-509f-4579-9c0a-1ef8181ab839', '11111111-1111-1111-1111-111111111103', 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:27.605308+00', '2026-06-08 12:27:27.605308+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.reunions (id, title, type, description, location, starts_at, ends_at, status, organizer_id, formation_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('fbabb0d7-fdea-470a-a54d-aa712a7b1360', 'Webinaire Master Data Science - Partenariat Sonatel', 'webinaire', 'Presentation des projets de fin d etudes avec le partenaire Sonatel.', 'https://visio.unchk.sn/webinaire-data2026', '2026-04-10 10:00:00+00', '2026-04-10 11:30:00+00', 'planifiee', '442f2765-25fb-4cda-a1d7-ecdce1c2bdec', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:27.654285+00', '2026-06-08 12:27:27.654285+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.reunions (id, title, type, description, location, starts_at, ends_at, status, organizer_id, formation_ref, version, created_by, created_at, updated_at, deleted_at) VALUES ('d3941da7-8076-467a-b7e8-ec6f63fd98f4', 'Reunion de coordination - Licence Developpement Web', 'reunion', 'Suivi des enseignements et planification des soutenances.', 'Salle B12 - Campus numerique Thies', '2026-03-05 08:30:00+00', '2026-03-05 10:00:00+00', 'planifiee', 'cf3b3942-919b-4c04-8b6c-ed23bcf92f9e', NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:27.701247+00', '2026-06-08 12:27:27.701247+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: comptes_rendus; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.comptes_rendus (id, reunion_id, title, type, body, document_ref, meeting_date, author_id, is_published, published_at, version, created_by, created_at, updated_at, deleted_at) VALUES ('ddaf528d-6b85-441e-a283-2333a972d27a', '27690a64-5420-4736-a325-64f9d46bd6ae', 'CR - Seminaire pedagogique Innovation numerique', 'seminaire', 'Synthese des ateliers sur l enseignement a distance. Recommandations sur les outils collaboratifs (forums, classes virtuelles).', NULL, '2026-05-20', '44e9da56-509f-4579-9c0a-1ef8181ab839', false, NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:32.517927+00', '2026-06-08 12:28:32.517927+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.comptes_rendus (id, reunion_id, title, type, body, document_ref, meeting_date, author_id, is_published, published_at, version, created_by, created_at, updated_at, deleted_at) VALUES ('80f5fb3e-b77f-43e1-b7ae-50e80d7eb598', NULL, 'CR - Reunion de coordination Licence Dev Web', 'reunion', 'Planification des soutenances de juin. Repartition des jurys. Suivi des memoires en retard.', NULL, '2026-03-05', '44e9da56-509f-4579-9c0a-1ef8181ab839', false, NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:32.561093+00', '2026-06-08 12:28:32.561093+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.comptes_rendus (id, reunion_id, title, type, body, document_ref, meeting_date, author_id, is_published, published_at, version, created_by, created_at, updated_at, deleted_at) VALUES ('d49fbd56-43dd-49b6-87d7-ee835a99e842', '216238cf-fd96-42be-b768-18bf103100eb', 'CR - Conseil d Universite Juin 2026', 'conseil_universite', 'Le Conseil a valide le budget 2026-2027 et l ouverture du Master Cybersecurite. Adoption du calendrier academique.', NULL, '2026-06-15', 'cf3b3942-919b-4c04-8b6c-ed23bcf92f9e', true, '2026-06-08 12:28:45.528557+00', 1, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:32.368971+00', '2026-06-08 12:28:45.53015+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.comptes_rendus (id, reunion_id, title, type, body, document_ref, meeting_date, author_id, is_published, published_at, version, created_by, created_at, updated_at, deleted_at) VALUES ('683e93cf-e17e-49aa-b849-9cf697dafbba', 'fbabb0d7-fdea-470a-a54d-aa712a7b1360', 'CR - Webinaire Master Data Science x Sonatel', 'webinaire', 'Presentation des projets etudiants. Sonatel propose 5 stages. Partenariat reconduit pour l annee 2026-2027.', NULL, '2026-04-10', '0caafe65-9f7a-488c-9c52-e6620b803d6e', true, '2026-06-08 12:28:45.559771+00', 1, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:32.54257+00', '2026-06-08 12:28:45.560522+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.comptes_rendus (id, reunion_id, title, type, body, document_ref, meeting_date, author_id, is_published, published_at, version, created_by, created_at, updated_at, deleted_at) VALUES ('a9010c54-c59d-46f5-bae6-25cc22f34ff5', NULL, 'CR temps reel test', 'conseil_universite', NULL, NULL, '2026-06-08', 'fbec7acb-d810-4327-b46a-876d8e6fee0d', true, '2026-06-08 14:15:54.731752+00', 1, '00000000-0000-0000-0000-000000000001', '2026-06-08 14:15:54.535005+00', '2026-06-08 14:15:54.733471+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: compte_rendu_visibility; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('d49fbd56-43dd-49b6-87d7-ee835a99e842', 'administratif') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('d49fbd56-43dd-49b6-87d7-ee835a99e842', 'admin') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('d49fbd56-43dd-49b6-87d7-ee835a99e842', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('ddaf528d-6b85-441e-a283-2333a972d27a', 'enseignant') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('ddaf528d-6b85-441e-a283-2333a972d27a', 'etudiant') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('683e93cf-e17e-49aa-b849-9cf697dafbba', 'administratif') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('683e93cf-e17e-49aa-b849-9cf697dafbba', 'appui-insertion') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('80f5fb3e-b77f-43e1-b7ae-50e80d7eb598', 'administratif') ON CONFLICT DO NOTHING;
INSERT INTO public.compte_rendu_visibility (compte_rendu_id, role) VALUES ('a9010c54-c59d-46f5-bae6-25cc22f34ff5', 'admin') ON CONFLICT DO NOTHING;


--
-- Data for Name: identity_user_ro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.identity_user_ro (id, full_name, email, roles, is_active, last_event_at, event_offset) VALUES ('00000000-0000-0000-0000-000000000001', 'Administrateur UNCHK', 'admin@unchk.sn', '{admin}', true, '2026-06-08 23:46:06.120807+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.identity_user_ro (id, full_name, email, roles, is_active, last_event_at, event_offset) VALUES ('fb218693-ca90-46e7-a70c-9393bf66318d', 'Awa Administrative', 'administratif@unchk.sn', '{administratif}', true, '2026-06-08 23:46:06.123437+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.identity_user_ro (id, full_name, email, roles, is_active, last_event_at, event_offset) VALUES ('0c303a78-3c7e-427d-825f-3f4105a8d9b1', 'Fatou Appui', 'appui@unchk.sn', '{appui-insertion}', true, '2026-06-08 23:46:06.123708+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.identity_user_ro (id, full_name, email, roles, is_active, last_event_at, event_offset) VALUES ('9ecf71cc-1939-4181-80c6-dec012ed2792', 'Cheikh Etudiant', 'etudiant@unchk.sn', '{etudiant}', true, '2026-06-08 23:46:06.123901+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.identity_user_ro (id, full_name, email, roles, is_active, last_event_at, event_offset) VALUES ('04cc91ea-4524-4e87-81f7-f1f60007cab0', 'Mamadou Enseignant', 'enseignant@unchk.sn', '{enseignant}', true, '2026-06-08 23:46:06.124077+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('2f46523e-67bb-48d0-9ad0-90eefaaaa4c7', '11111111-1111-1111-1111-111111111101', 'reunion', 'Convocation : Reunion de coordination - Licence Developpement Web', 'Vous êtes invité(e) à une réunion.', 'communication', 'd3941da7-8076-467a-b7e8-ec6f63fd98f4', false, NULL, true, '2026-06-08 12:27:29.488587+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('de293187-9c74-49d7-a50f-ae187e77ff18', '11111111-1111-1111-1111-111111111102', 'reunion', 'Convocation : Conseil d Universite - Session ordinaire Juin 2026', 'Vous êtes invité(e) à une réunion.', 'communication', '216238cf-fd96-42be-b768-18bf103100eb', false, NULL, true, '2026-06-08 12:27:29.495418+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('a14b9ccc-ff73-4f44-b2e2-8e3d486466d0', '22222222-2222-2222-2222-222222222201', 'reunion', 'Convocation : Webinaire Master Data Science - Partenariat Sonatel', 'Vous êtes invité(e) à une réunion.', 'communication', 'fbabb0d7-fdea-470a-a54d-aa712a7b1360', false, NULL, true, '2026-06-08 12:27:29.497962+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('64dade02-5e9f-4f8b-8ba5-71e078639944', '22222222-2222-2222-2222-222222222202', 'reunion', 'Convocation : Webinaire Master Data Science - Partenariat Sonatel', 'Vous êtes invité(e) à une réunion.', 'communication', 'fbabb0d7-fdea-470a-a54d-aa712a7b1360', false, NULL, true, '2026-06-08 12:27:29.501505+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('fbbafb3b-38ec-4250-9fe2-60275eca3396', '11111111-1111-1111-1111-111111111101', 'reunion', 'Convocation : Conseil d Universite - Session ordinaire Juin 2026', 'Vous êtes invité(e) à une réunion.', 'communication', '216238cf-fd96-42be-b768-18bf103100eb', false, NULL, true, '2026-06-08 12:27:29.505079+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('19f8c640-7772-42de-a980-a9f3d6bac4f0', '11111111-1111-1111-1111-111111111102', 'reunion', 'Convocation : Seminaire pedagogique - Innovation numerique', 'Vous êtes invité(e) à une réunion.', 'communication', '27690a64-5420-4736-a325-64f9d46bd6ae', false, NULL, true, '2026-06-08 12:27:29.513393+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('63dd87ea-439d-4273-b86d-921b5c4133eb', '22222222-2222-2222-2222-222222222201', 'reunion', 'Convocation : Seminaire pedagogique - Innovation numerique', 'Vous êtes invité(e) à une réunion.', 'communication', '27690a64-5420-4736-a325-64f9d46bd6ae', false, NULL, true, '2026-06-08 12:27:29.517279+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('e62e2825-c223-4eb1-a6c6-5625753e3b8c', '22222222-2222-2222-2222-222222222202', 'reunion', 'Convocation : Seminaire pedagogique - Innovation numerique', 'Vous êtes invité(e) à une réunion.', 'communication', '27690a64-5420-4736-a325-64f9d46bd6ae', false, NULL, true, '2026-06-08 12:27:29.521507+00') ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (id, recipient_id, kind, title, message, target_service, target_ref, is_read, read_at, delivered_ws, created_at) VALUES ('ac05ac2d-7157-444b-87dd-ebead5137c03', 'fb218693-ca90-46e7-a70c-9393bf66318d', 'compte_rendu', 'Nouveau compte rendu : CR Notif Test', 'Un compte rendu a été publié.', 'communication', 'cb417c0e-c464-404e-a177-6b66f2311945', false, NULL, false, '2026-06-08 23:46:56.689346+00') ON CONFLICT DO NOTHING;


--
-- Data for Name: people_staff_ro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('0caafe65-9f7a-488c-9c52-e6620b803d6e', 'Khady Faye', 'enseignant', '2026-06-08 23:46:06.242795+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('fbec7acb-d810-4327-b46a-876d8e6fee0d', 'Moussa Cisse', 'enseignant', '2026-06-08 23:46:06.244015+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('77b0fc11-5b06-41b7-afbf-dc14c06eead6', 'Aminata Niang', 'responsable_formation', '2026-06-08 23:46:06.244251+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('cf3b3942-919b-4c04-8b6c-ed23bcf92f9e', 'Ibrahima Thiam', 'tuteur', '2026-06-08 23:46:06.244481+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('cfa0fa36-c3d9-433e-9b11-1e97ddc23485', 'Mariama Diop', 'administratif', '2026-06-08 23:46:06.244724+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('44e9da56-509f-4579-9c0a-1ef8181ab839', 'Seydou Ba', 'enseignant_associe', '2026-06-08 23:46:06.24494+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('a0e0537d-97cf-4354-84e2-5cd72ded36b0', 'Fatou Sow', 'appui_insertion', '2026-06-08 23:46:06.245119+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.people_staff_ro (id, full_name, kind, last_event_at, event_offset) VALUES ('442f2765-25fb-4cda-a1d7-ecdce1c2bdec', 'Cheikh Gueye', 'enseignant', '2026-06-08 23:46:06.245273+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: reunion_participants; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('216238cf-fd96-42be-b768-18bf103100eb', '11111111-1111-1111-1111-111111111101', 'staff', true) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('216238cf-fd96-42be-b768-18bf103100eb', '11111111-1111-1111-1111-111111111102', 'staff', false) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('27690a64-5420-4736-a325-64f9d46bd6ae', '11111111-1111-1111-1111-111111111102', 'staff', true) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('27690a64-5420-4736-a325-64f9d46bd6ae', '22222222-2222-2222-2222-222222222201', 'student', true) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('27690a64-5420-4736-a325-64f9d46bd6ae', '22222222-2222-2222-2222-222222222202', 'student', false) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('fbabb0d7-fdea-470a-a54d-aa712a7b1360', '22222222-2222-2222-2222-222222222201', 'student', true) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('fbabb0d7-fdea-470a-a54d-aa712a7b1360', '22222222-2222-2222-2222-222222222202', 'student', true) ON CONFLICT DO NOTHING;
INSERT INTO public.reunion_participants (reunion_id, person_ref, person_kind, is_present) VALUES ('d3941da7-8076-467a-b7e8-ec6f63fd98f4', '11111111-1111-1111-1111-111111111101', 'staff', true) ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
