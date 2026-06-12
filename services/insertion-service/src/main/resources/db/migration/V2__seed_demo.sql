-- ============================================================
-- Données de DÉMONSTRATION — insertion-service.
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
-- Data for Name: academic_formation_ro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('3f33149d-1fe9-424e-b4ac-f0b2301ed6b0', 'Formation Continue Multimédia', 'formation_continue', '2026-06-08 12:33:22.470183+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('e9313d0a-b73d-4160-ac4e-d5d119b61a35', 'Licence Réseaux & Télécoms', 'licence', '2026-06-08 12:33:22.470211+00', 3) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('8def8e75-ccb6-432c-be54-0a9d233855da', 'Certificat Cybersécurité', 'certificat', '2026-06-08 12:33:22.470153+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('9d333483-dfe3-48dd-ae76-80999934de8e', 'Licence Développement Web', 'licence', '2026-06-08 12:33:22.470198+00', 3) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'Master Data Science', 'master', '2026-06-08 12:33:22.470223+00', 4) ON CONFLICT DO NOTHING;
INSERT INTO public.academic_formation_ro (id, label, level, last_event_at, event_offset) VALUES ('87424228-2331-46a5-b31b-583c21722562', 'Master Génie Logiciel', 'master', '2026-06-08 12:33:22.470234+00', 5) ON CONFLICT DO NOTHING;


--
-- Data for Name: contact_log; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('16fcd397-08d7-4971-bf67-2fb176b0795c', '53984ae7-1e63-4689-b273-1745cc49e0c5', '2024-10-02', 'telephone', 'Confirmation de l''embauche chez Sonatel. Diplômé satisfait de son poste d''ingénieur logiciel.', '0caafe65-9f7a-488c-9c52-e6620b803d6e', '2026-06-08 12:34:43.299285+00') ON CONFLICT DO NOTHING;
INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('032fd749-7bd6-4771-afb7-eacf45f70d22', 'a914eece-9631-4119-b928-4987d510714d', '2024-11-15', 'email', 'Accompagnement à la création de son entreprise (auto-emploi). Orientation vers l''ADEPME.', 'fbec7acb-d810-4327-b46a-876d8e6fee0d', '2026-06-08 12:34:43.320539+00') ON CONFLICT DO NOTHING;
INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('d7fb08d0-2908-432f-beb6-1aa5edd7557f', 'fa07ec0b-6ab1-4c65-8a11-0229de031379', '2025-01-12', 'presentiel', 'Entretien de suivi - en recherche d''emploi. Mise en relation avec le réseau des partenaires.', '77b0fc11-5b06-41b7-afbf-dc14c06eead6', '2026-06-08 12:34:43.332689+00') ON CONFLICT DO NOTHING;
INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('efcdd467-f4e4-437f-b58b-eb8be990682e', 'e2b400f6-cce1-4cbc-aa17-3a4af6a63197', '2025-02-25', 'telephone', 'Diplômé recruté comme Data Engineer chez Orange Sénégal. Bilan d''insertion positif.', '0caafe65-9f7a-488c-9c52-e6620b803d6e', '2026-06-08 12:34:43.344954+00') ON CONFLICT DO NOTHING;
INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('d8c94ace-6851-40a7-b4ad-012a1c1aed42', '5cf1f55b-5fd6-47ad-ac53-712f8b95e504', '2025-02-01', 'email', 'Relance pour mise à jour de la situation. Sans activité déclarée, proposition d''atelier CV.', 'fbec7acb-d810-4327-b46a-876d8e6fee0d', '2026-06-08 12:34:43.356111+00') ON CONFLICT DO NOTHING;
INSERT INTO public.contact_log (id, student_ref, contacted_at, channel, notes, agent_ref, created_at) VALUES ('ef2223b6-52f5-4c8a-9b9d-f508fee7b428', '01091039-493c-4d60-94ea-1573ddbfd973', '2025-05-05', 'presentiel', 'Atelier de préparation aux entretiens. Candidate motivée, profil développement web.', '77b0fc11-5b06-41b7-afbf-dc14c06eead6', '2026-06-08 12:34:43.366589+00') ON CONFLICT DO NOTHING;


--
-- Data for Name: insertion_outcomes; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('33bf9342-83b5-4adc-9ed6-7846fef9b8dc', '53984ae7-1e63-4689-b273-1745cc49e0c5', 'e9313d0a-b73d-4160-ac4e-d5d119b61a35', 'emploi_salarie', 'Sonatel', 'Ingénieur Logiciel', '2024-09-15', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.341067+00', '2026-06-08 12:34:26.341067+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('837d32fd-0da9-42b4-90e1-5f96b66a5e78', 'a914eece-9631-4119-b928-4987d510714d', '87424228-2331-46a5-b31b-583c21722562', 'auto_emploi', 'Sarr Digital Services', 'Fondatrice / Développeuse Freelance', '2024-10-01', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.388367+00', '2026-06-08 12:34:26.388367+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('b9054bf4-46a3-42be-8a68-b6508a776696', 'e2b400f6-cce1-4cbc-aa17-3a4af6a63197', '87424228-2331-46a5-b31b-583c21722562', 'emploi_salarie', 'Orange Sénégal', 'Data Engineer', '2025-02-20', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.411451+00', '2026-06-08 12:34:26.411451+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('c53bf222-b377-43aa-a3c9-5d55c1b79d8a', '187d3f1c-907a-4280-a24c-333236812d9d', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'poursuite_etudes', NULL, 'Master Data Science (poursuite)', '2024-11-05', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.4316+00', '2026-06-08 12:34:26.4316+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('84dfecc4-06a5-4bb4-a24e-37922596d704', 'fa07ec0b-6ab1-4c65-8a11-0229de031379', '8def8e75-ccb6-432c-be54-0a9d233855da', 'recherche_emploi', NULL, NULL, '2025-01-10', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.449337+00', '2026-06-08 12:34:26.449337+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('127bf44e-d43c-4d6e-bd9c-2ed2f159ced0', '22206b9a-9100-4670-8aa9-ff41959bbb3d', '9d333483-dfe3-48dd-ae76-80999934de8e', 'emploi_salarie', 'Sénégal Numérique SA', 'Administrateur Systèmes & Réseaux', '2025-03-12', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.463932+00', '2026-06-08 12:34:26.463932+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('57bbddb8-b9c3-4eb3-8fc3-55e01a87a914', '7a0e1a97-4cb1-424c-89dc-20469fe90f76', '9d333483-dfe3-48dd-ae76-80999934de8e', 'auto_emploi', 'Atelier Web Aminata', 'Entrepreneuse - Agence Web', '2024-12-18', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.47467+00', '2026-06-08 12:34:26.47467+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('2b94aa8b-590b-4478-9527-e7f93abd63aa', '5cf1f55b-5fd6-47ad-ac53-712f8b95e504', '3f33149d-1fe9-424e-b4ac-f0b2301ed6b0', 'sans_activite', NULL, NULL, '2025-01-25', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.485711+00', '2026-06-08 12:34:26.485711+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('57ad9979-cb95-4186-a680-85f5582f6c4c', '7964b745-4fe3-4db8-aa9f-c6b905b2bf42', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', 'emploi_salarie', 'Wave', 'Développeur Mobile', '2025-04-08', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.497531+00', '2026-06-08 12:34:26.497531+00') ON CONFLICT DO NOTHING;
INSERT INTO public.insertion_outcomes (id, student_ref, formation_ref, kind, employer_name, job_title, observed_at, is_current, created_by, created_at, updated_at) VALUES ('bd51924e-87e3-484e-b1c0-d6866a513866', '01091039-493c-4d60-94ea-1573ddbfd973', 'e9313d0a-b73d-4160-ac4e-d5d119b61a35', 'recherche_emploi', NULL, NULL, '2025-05-02', true, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:26.508361+00', '2026-06-08 12:34:26.508361+00') ON CONFLICT DO NOTHING;


--
-- Data for Name: partners; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.partners (id, name, kind, sector, contact_name, contact_email, contact_phone, address, city, is_active, version, created_by, created_at, updated_at, deleted_at) VALUES ('80ab4f7c-05f1-4235-9742-5a18e2a7eade', 'Wave', 'entreprise', 'Fintech / Paiement mobile', 'Fatou Fall', 'fatou.fall@wave.com', '+221761234567', 'Almadies, Dakar', 'Dakar', true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:28:56.679898+00', '2026-06-08 12:28:56.679898+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.partners (id, name, kind, sector, contact_name, contact_email, contact_phone, address, city, is_active, version, created_by, created_at, updated_at, deleted_at) VALUES ('e675d921-36cb-47cb-9c88-f497bb7139ec', 'Sénégal Numérique SA', 'institution', 'Numérique / Service public', 'Cheikh Sow', 'cheikh.sow@senum.sn', '+221338690000', 'Diamniadio', 'Diamniadio', true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:29:55.634541+00', '2026-06-08 12:29:55.634541+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.partners (id, name, kind, sector, contact_name, contact_email, contact_phone, address, city, is_active, version, created_by, created_at, updated_at, deleted_at) VALUES ('9dbe6fdd-b186-4695-87a5-fff6c98c418c', 'PayDunya', 'entreprise', 'Fintech', 'Aminata Ba', 'aminata.ba@paydunya.com', '+221771112233', 'Mermoz, Dakar', 'Dakar', true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:29:55.689983+00', '2026-06-08 12:29:55.689983+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.partners (id, name, kind, sector, contact_name, contact_email, contact_phone, address, city, is_active, version, created_by, created_at, updated_at, deleted_at) VALUES ('fe72d6ea-4b23-45c5-8ffc-2134885402f1', 'Sonatel', 'entreprise', 'Télécommunications', 'Awa Diop', 'awa.diop@sonatel.sn', '+221338391212', 'Route du Méridien Président, Dakar', 'Dakar', true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:30:08.373817+00', '2026-06-08 12:30:08.373817+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.partners (id, name, kind, sector, contact_name, contact_email, contact_phone, address, city, is_active, version, created_by, created_at, updated_at, deleted_at) VALUES ('335bb4eb-b16e-4230-a1eb-f08a65ad3a54', 'Orange Sénégal', 'entreprise', 'Télécommunications', 'Mamadou Ndiaye', 'mamadou.ndiaye@orange.sn', '+221338000000', 'VDN, Dakar', 'Dakar', true, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:30:08.460361+00', '2026-06-08 12:30:08.460361+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: internships; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.internships (id, student_ref, partner_id, title, start_date, end_date, status, tutor_ref, supervisor_name, report_ref, grade, version, created_by, created_at, updated_at, deleted_at) VALUES ('6274f4bd-c56d-44a0-ab20-49ca0035634e', '53984ae7-1e63-4689-b273-1745cc49e0c5', 'fe72d6ea-4b23-45c5-8ffc-2134885402f1', 'Stage Développeur Backend Java - Plateforme USSD', '2025-01-06', '2025-04-04', 'valide', '0caafe65-9f7a-488c-9c52-e6620b803d6e', 'Ousmane Gueye', NULL, 17.50, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:09.652622+00', '2026-06-08 12:34:09.652622+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.internships (id, student_ref, partner_id, title, start_date, end_date, status, tutor_ref, supervisor_name, report_ref, grade, version, created_by, created_at, updated_at, deleted_at) VALUES ('9dcaa26a-e40e-4b82-a833-d8a1fd47bb2c', 'e2b400f6-cce1-4cbc-aa17-3a4af6a63197', '335bb4eb-b16e-4230-a1eb-f08a65ad3a54', 'Stage Ingénieur Data - Pipeline analytics', '2025-02-03', '2025-05-30', 'termine', 'fbec7acb-d810-4327-b46a-876d8e6fee0d', 'Aïssatou Diallo', NULL, 15.00, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:09.730817+00', '2026-06-08 12:34:09.730817+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.internships (id, student_ref, partner_id, title, start_date, end_date, status, tutor_ref, supervisor_name, report_ref, grade, version, created_by, created_at, updated_at, deleted_at) VALUES ('af96c834-bbc0-4bc7-8a67-f6186b8d5df9', '187d3f1c-907a-4280-a24c-333236812d9d', '80ab4f7c-05f1-4235-9742-5a18e2a7eade', 'Stage Développeuse Mobile - App de paiement', '2025-03-10', '2025-06-06', 'en_cours', '77b0fc11-5b06-41b7-afbf-dc14c06eead6', 'Ibrahima Sarr', NULL, NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:09.755395+00', '2026-06-08 12:34:09.755395+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.internships (id, student_ref, partner_id, title, start_date, end_date, status, tutor_ref, supervisor_name, report_ref, grade, version, created_by, created_at, updated_at, deleted_at) VALUES ('2a8b8cbb-ada6-4650-bd52-bfa6432d3e3e', '22206b9a-9100-4670-8aa9-ff41959bbb3d', 'e675d921-36cb-47cb-9c88-f497bb7139ec', 'Stage Administrateur Réseaux & Télécoms', '2025-04-01', '2025-07-31', 'prevu', 'cf3b3942-919b-4c04-8b6c-ed23bcf92f9e', 'Moussa Faye', NULL, NULL, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:09.77276+00', '2026-06-08 12:34:09.77276+00', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.internships (id, student_ref, partner_id, title, start_date, end_date, status, tutor_ref, supervisor_name, report_ref, grade, version, created_by, created_at, updated_at, deleted_at) VALUES ('a6e288c3-7fe1-4ae2-abc9-e078bd4ac1ab', 'a914eece-9631-4119-b928-4987d510714d', '9dbe6fdd-b186-4695-87a5-fff6c98c418c', 'Stage Analyste Cybersécurité - Audit & pentest', '2024-09-02', '2024-12-20', 'valide', 'cfa0fa36-c3d9-433e-9b11-1e97ddc23485', 'Khady Niang', NULL, 18.00, 0, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:34:09.798168+00', '2026-06-08 12:34:09.798168+00', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: people_student_ro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('7a0e1a97-4cb1-424c-89dc-20469fe90f76', 'Aminata Ba', 'femme', '9d333483-dfe3-48dd-ae76-80999934de8e', '2023-2024', NULL, '2026-06-08 12:33:06.765521+00', 4) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('fa07ec0b-6ab1-4c65-8a11-0229de031379', 'Aissatou Gueye', 'femme', '8def8e75-ccb6-432c-be54-0a9d233855da', '2021-2022', NULL, '2026-06-08 12:33:06.765765+00', 5) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('a914eece-9631-4119-b928-4987d510714d', 'Mariama Sarr', 'femme', '87424228-2331-46a5-b31b-583c21722562', '2022-2023', 2024, '2026-06-08 12:33:06.765796+00', 6) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('53984ae7-1e63-4689-b273-1745cc49e0c5', 'Cheikh Sow', 'homme', 'e9313d0a-b73d-4160-ac4e-d5d119b61a35', '2022-2023', 2024, '2026-06-08 12:33:06.765806+00', 7) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('e2b400f6-cce1-4cbc-aa17-3a4af6a63197', 'Ibrahima Diallo', 'homme', '87424228-2331-46a5-b31b-583c21722562', '2024-2025', NULL, '2026-06-08 12:33:06.76573+00', 5) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('01091039-493c-4d60-94ea-1573ddbfd973', 'Awa Diop', 'femme', 'e9313d0a-b73d-4160-ac4e-d5d119b61a35', '2024-2025', NULL, '2026-06-08 12:33:06.765744+00', 6) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('5cf1f55b-5fd6-47ad-ac53-712f8b95e504', 'Seydou Mbaye', 'homme', '3f33149d-1fe9-424e-b4ac-f0b2301ed6b0', '2023-2024', NULL, '2026-06-08 12:33:06.765775+00', 7) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('7964b745-4fe3-4db8-aa9f-c6b905b2bf42', 'Ousmane Sy', 'homme', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', '2024-2025', NULL, '2026-06-08 12:33:06.765817+00', 8) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('bf617f89-075d-4b92-87cb-e807fd5cf49f', 'Verif Test', 'autre', '8def8e75-ccb6-432c-be54-0a9d233855da', NULL, NULL, '2026-06-08 12:33:06.765826+00', 9) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('187d3f1c-907a-4280-a24c-333236812d9d', 'Fatou Fall', 'femme', '1d12ce3b-d4d9-4c5f-99f5-bbe8b16414ac', '2023-2024', NULL, '2026-06-08 12:33:06.765755+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.people_student_ro (id, full_name, gender, formation_ref, promotion, exit_year, last_event_at, event_offset) VALUES ('22206b9a-9100-4670-8aa9-ff41959bbb3d', 'Mamadou Ndiaye', 'homme', '9d333483-dfe3-48dd-ae76-80999934de8e', '2024-2025', NULL, '2026-06-08 12:33:06.765785+00', 3) ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
