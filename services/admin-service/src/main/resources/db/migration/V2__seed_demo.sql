-- ============================================================
-- Données de DÉMONSTRATION — admin-service.
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
-- Data for Name: budgets; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.budgets (id, fiscal_year, label, status, orientation_note, total_planned, total_realized, currency, owner_id, version, created_by, created_at, updated_at) VALUES ('3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 2024, 'Budget de fonctionnement 2024', 'cloture', 'Consolidation des plateformes pédagogiques et montée en charge des ENO. Priorité à la connectivité et aux licences logicielles.', 1875000000.00, 1788700000.00, 'XOF', '00000000-0000-0000-0000-000000000001', 15, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:26:40.682534+00', '2026-06-08 12:28:27.437692+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budgets (id, fiscal_year, label, status, orientation_note, total_planned, total_realized, currency, owner_id, version, created_by, created_at, updated_at) VALUES ('c2a20909-316c-4e07-934f-5d5fbee42929', 2025, 'Budget de fonctionnement 2025', 'en_execution', 'Extension du réseau des espaces numériques ouverts et renforcement de la cybersécurité.', 2150000000.00, 1372200000.00, 'XOF', '00000000-0000-0000-0000-000000000001', 17, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:02.065525+00', '2026-06-08 12:28:27.730763+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budgets (id, fiscal_year, label, status, orientation_note, total_planned, total_realized, currency, owner_id, version, created_by, created_at, updated_at) VALUES ('2bd14bc6-1e86-446b-9155-ad17c4956fd0', 2026, 'Budget de fonctionnement 2026', 'vote', 'Déploiement de la plateforme Data Science et partenariats Sonatel/Wave pour les paiements en ligne. Soutien à la formation continue.', 2300000000.00, 26700000.00, 'XOF', '00000000-0000-0000-0000-000000000001', 10, '00000000-0000-0000-0000-000000000001', '2026-06-08 12:27:25.433606+00', '2026-06-08 12:28:27.822752+00') ON CONFLICT DO NOTHING;


--
-- Data for Name: budget_lines; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('94270475-b5f1-4d63-8cc4-ac3ef8ca9298', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Recettes - Subvention de l''État', 'recette', 980000000.00, 0.00, 'Dotation budgétaire annuelle UNCHK', '2026-06-08 12:27:49.520783+00', '2026-06-08 12:27:49.520783+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('77c1cb4a-294d-4b3c-ae6f-45a92a71a0d6', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Recettes - Partenariat Wave & PayDunya', 'recette', 75000000.00, 0.00, 'Paiements en ligne Wave et PayDunya', '2026-06-08 12:27:49.575435+00', '2026-06-08 12:27:49.575435+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('a328b7d5-6bad-4f8c-9ad8-02822d094fea', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Dépenses - Connectivité & Internet', 'depense', 225000000.00, 0.00, 'Liaisons Sénégal Numérique SA', '2026-06-08 12:27:49.638696+00', '2026-06-08 12:27:49.638696+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('81bd3784-421f-4370-83ba-a60eeee23d92', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Dépenses - Masse salariale', 'depense', 610000000.00, 0.00, 'Personnel enseignant et administratif', '2026-06-08 12:27:49.666675+00', '2026-06-08 12:27:49.666675+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('81f65950-ed1e-44ca-a921-a887a65e18a6', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Dépenses - Cybersécurité', 'depense', 100000000.00, 0.00, 'Renforcement Master Cybersécurité', '2026-06-08 12:27:49.690516+00', '2026-06-08 12:27:49.690516+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('937fd1f3-6907-44a6-8991-e9601e7774e6', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Recettes - Subvention de l''État', 'recette', 850000000.00, 833000000.00, 'Dotation budgétaire annuelle UNCHK', '2026-06-08 12:27:48.968283+00', '2026-06-08 12:28:27.195325+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('da773112-de39-40f0-9dc6-5024da887a5a', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Dépenses - Masse salariale', 'depense', 520000000.00, 473200000.00, 'Personnel enseignant et administratif', '2026-06-08 12:27:49.205384+00', '2026-06-08 12:28:27.255429+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('32003f39-2a54-42b3-8895-5899844485f6', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Dépenses - Connectivité & Internet', 'depense', 180000000.00, 180000000.00, 'Liaisons fibre ENO et data centers', '2026-06-08 12:27:49.124221+00', '2026-06-08 12:28:27.284742+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('330ea709-a516-4bf2-aea9-6e5bea1ee58b', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Recettes - Frais d''inscription', 'recette', 120000000.00, 114000000.00, 'Droits d''inscription étudiants', '2026-06-08 12:27:49.040403+00', '2026-06-08 12:28:27.315768+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('22e60500-5423-4248-b9ef-1f56178934b6', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Dépenses - Licences logicielles', 'depense', 95000000.00, 83600000.00, 'Licences LMS, visioconférence, antivirus', '2026-06-08 12:27:49.167151+00', '2026-06-08 12:28:27.350418+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('980de5d2-a9c3-4f98-aa11-59e7228c8982', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Dépenses - Maintenance équipements', 'depense', 65000000.00, 63050000.00, 'Maintenance serveurs et postes ENO', '2026-06-08 12:27:49.237124+00', '2026-06-08 12:28:27.384348+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('d3332513-25e3-40fb-a964-02db5d07866a', '3fa7ffd2-90ec-4c8f-ae05-fc0cc7862608', 'Recettes - Partenariat Sonatel', 'recette', 45000000.00, 41850000.00, 'Convention connectivité Sonatel', '2026-06-08 12:27:49.082048+00', '2026-06-08 12:28:27.409116+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('68f4b861-3979-4423-aca3-1b68939141ba', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Recettes - Subvention de l''État', 'recette', 920000000.00, 570400000.00, 'Dotation budgétaire annuelle UNCHK', '2026-06-08 12:27:49.26959+00', '2026-06-08 12:28:27.484866+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('3277c7b0-093b-446d-988e-92befbd74035', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Dépenses - Masse salariale', 'depense', 560000000.00, 392000000.00, 'Personnel enseignant et administratif', '2026-06-08 12:27:49.426994+00', '2026-06-08 12:28:27.507453+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('17e0afb7-8ac9-4959-a606-d4488cbe457c', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Dépenses - Connectivité & Internet', 'depense', 210000000.00, 115500000.00, 'Montée en débit des ENO', '2026-06-08 12:27:49.368634+00', '2026-06-08 12:28:27.532644+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('19cb8a8a-7a27-49e4-baab-3ebfffce2190', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Recettes - Frais d''inscription', 'recette', 145000000.00, 98600000.00, 'Droits d''inscription étudiants', '2026-06-08 12:27:49.301377+00', '2026-06-08 12:28:27.594912+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('f6adcb5a-36a7-40ce-a6d2-38cffa4abfab', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Dépenses - Équipement informatique', 'depense', 130000000.00, 78000000.00, 'Renouvellement parc serveurs Atos Sénégal', '2026-06-08 12:27:49.455103+00', '2026-06-08 12:28:27.620867+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('2f02967d-e681-45c2-a746-915bb0366891', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Dépenses - Cybersécurité', 'depense', 85000000.00, 62900000.00, 'SOC, audits et licences sécurité', '2026-06-08 12:27:49.399835+00', '2026-06-08 12:28:27.643294+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('bab128ac-97f6-4943-a031-a2d797d97838', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Recettes - Partenariat Orange Sénégal', 'recette', 60000000.00, 34800000.00, 'Convention Orange Sénégal', '2026-06-08 12:27:49.33447+00', '2026-06-08 12:28:27.670804+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('388383fe-bc6b-4168-bc9d-2dad66c4d6b5', 'c2a20909-316c-4e07-934f-5d5fbee42929', 'Dépenses - Formation continue', 'depense', 40000000.00, 20000000.00, 'Certifications Data Science et Génie Logiciel', '2026-06-08 12:27:49.490483+00', '2026-06-08 12:28:27.698136+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('b51ebdde-95b2-4ad8-b241-6a3d66379b60', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Recettes - Frais d''inscription', 'recette', 160000000.00, 19200000.00, 'Droits d''inscription étudiants', '2026-06-08 12:27:49.551207+00', '2026-06-08 12:28:27.772465+00') ON CONFLICT DO NOTHING;
INSERT INTO public.budget_lines (id, budget_id, category, direction, planned_amount, realized_amount, label, created_at, updated_at) VALUES ('19f22e3c-a865-45dc-8003-27b47aab305e', '2bd14bc6-1e86-446b-9155-ad17c4956fd0', 'Dépenses - Plateforme Data Science', 'depense', 150000000.00, 7500000.00, 'Infrastructure GPU et stockage', '2026-06-08 12:27:49.60763+00', '2026-06-08 12:28:27.796401+00') ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--



SET session_replication_role = DEFAULT;
