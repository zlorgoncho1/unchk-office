-- ============================================================
-- Données de DÉMONSTRATION — identity-service.
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
-- Data for Name: read_person; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('7a0e1a97-4cb1-424c-89dc-20469fe90f76', 'etudiant', 'Aminata Ba', NULL, '2026-06-08 12:33:42.802784+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('fa07ec0b-6ab1-4c65-8a11-0229de031379', 'etudiant', 'Aissatou Gueye', NULL, '2026-06-08 12:33:42.818058+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('a914eece-9631-4119-b928-4987d510714d', 'etudiant', 'Mariama Sarr', NULL, '2026-06-08 12:33:42.820155+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('53984ae7-1e63-4689-b273-1745cc49e0c5', 'etudiant', 'Cheikh Sow', NULL, '2026-06-08 12:33:42.823484+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('e2b400f6-cce1-4cbc-aa17-3a4af6a63197', 'etudiant', 'Ibrahima Diallo', NULL, '2026-06-08 12:33:42.826147+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('01091039-493c-4d60-94ea-1573ddbfd973', 'etudiant', 'Awa Diop', NULL, '2026-06-08 12:33:42.828635+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('5cf1f55b-5fd6-47ad-ac53-712f8b95e504', 'etudiant', 'Seydou Mbaye', NULL, '2026-06-08 12:33:42.831105+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('7964b745-4fe3-4db8-aa9f-c6b905b2bf42', 'etudiant', 'Ousmane Sy', NULL, '2026-06-08 12:33:42.83319+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('bf617f89-075d-4b92-87cb-e807fd5cf49f', 'etudiant', 'Verif Test', NULL, '2026-06-08 12:33:42.834851+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('187d3f1c-907a-4280-a24c-333236812d9d', 'etudiant', 'Fatou Fall', NULL, '2026-06-08 12:33:42.836627+00') ON CONFLICT DO NOTHING;
INSERT INTO public.read_person (id, person_kind, full_name, email, updated_at) VALUES ('22206b9a-9100-4670-8aa9-ff41959bbb3d', 'etudiant', 'Mamadou Ndiaye', NULL, '2026-06-08 12:33:42.839145+00') ON CONFLICT DO NOTHING;


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.users (id, email, password_hash, full_name, person_ref, person_kind, is_active, is_locked, failed_attempts, last_login_at, version, created_at, updated_at, deleted_at) VALUES ('04cc91ea-4524-4e87-81f7-f1f60007cab0', 'enseignant@unchk.sn', '$2a$10$RqAIIVfBTxUJdF.QEpsv8OnE8W6v128vyEZhviRyDaiBji/JzA1f.', 'Mamadou Enseignant', '0caafe65-9f7a-488c-9c52-e6620b803d6e', 'personnel', true, false, 0, '2026-06-12 02:48:27.507368+00', 32, '2026-06-08 15:30:46.285938+00', '2026-06-12 02:48:27.507377+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.users (id, email, password_hash, full_name, person_ref, person_kind, is_active, is_locked, failed_attempts, last_login_at, version, created_at, updated_at, deleted_at) VALUES ('9ecf71cc-1939-4181-80c6-dec012ed2792', 'etudiant@unchk.sn', '$2a$10$T4VPZ5LFDHI7J2Yc93vuQ.2L88MUbQbGDfEoPlrKeGk5Djm/F2bUe', 'Awa Diop', '01091039-493c-4d60-94ea-1573ddbfd973', 'etudiant', true, false, 0, '2026-06-12 02:52:44.977266+00', 58, '2026-06-08 15:30:46.5174+00', '2026-06-12 02:52:44.977274+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.users (id, email, password_hash, full_name, person_ref, person_kind, is_active, is_locked, failed_attempts, last_login_at, version, created_at, updated_at, deleted_at) VALUES ('fb218693-ca90-46e7-a70c-9393bf66318d', 'administratif@unchk.sn', '$2a$10$G/hjscf1tT.ZqY7sMoigZeeRLeZCwpCItWNUKtER3Ey38SiqwvhI.', 'Awa Administrative', NULL, NULL, true, false, 0, '2026-06-12 02:52:50.92824+00', 39, '2026-06-08 15:30:46.091194+00', '2026-06-12 02:52:50.928249+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.users (id, email, password_hash, full_name, person_ref, person_kind, is_active, is_locked, failed_attempts, last_login_at, version, created_at, updated_at, deleted_at) VALUES ('0c303a78-3c7e-427d-825f-3f4105a8d9b1', 'appui@unchk.sn', '$2a$10$cTu6P2GtqXAPVcQKxZqnwe.Lcni9EbcX1ufo50PnZW2U509iJNPlm', 'Fatou Appui', NULL, NULL, true, false, 0, '2026-06-12 02:54:49.953819+00', 32, '2026-06-08 15:30:46.411106+00', '2026-06-12 02:54:49.953828+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.users (id, email, password_hash, full_name, person_ref, person_kind, is_active, is_locked, failed_attempts, last_login_at, version, created_at, updated_at, deleted_at) VALUES ('00000000-0000-0000-0000-000000000001', 'admin@unchk.sn', '$2a$12$e/N3M4LHPVK8oIgVymn08OUKcYmukDJmoqaXgLZD/JPJ0mdZdGFV6', 'Administrateur UNCHK', NULL, NULL, true, false, 0, '2026-06-12 03:04:02.579261+00', 183, '2026-06-08 09:59:33.269811+00', '2026-06-12 03:04:02.579275+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.user_roles (user_id, role, granted_at, granted_by) VALUES ('00000000-0000-0000-0000-000000000001', 'admin', '2026-06-08 09:59:33.273217+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role, granted_at, granted_by) VALUES ('fb218693-ca90-46e7-a70c-9393bf66318d', 'administratif', '2026-06-08 15:30:46.093307+00', '00000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role, granted_at, granted_by) VALUES ('04cc91ea-4524-4e87-81f7-f1f60007cab0', 'enseignant', '2026-06-08 15:30:46.287432+00', '00000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role, granted_at, granted_by) VALUES ('0c303a78-3c7e-427d-825f-3f4105a8d9b1', 'appui-insertion', '2026-06-08 15:30:46.412603+00', '00000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role, granted_at, granted_by) VALUES ('9ecf71cc-1939-4181-80c6-dec012ed2792', 'etudiant', '2026-06-08 18:24:08.786587+00', '00000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
