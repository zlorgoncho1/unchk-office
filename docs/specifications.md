# Spécifications fonctionnelles — UNCHK Office

Plateforme web de gestion administrative et pédagogique de l'**Université Cheikh Hamidou Kane**
(ex-UVS). Ce document décrit, pour chaque module métier, son objectif, ses entités, ses
principaux cas d'usage et les rôles concernés. Il se termine par la **matrice rôles × modules**.

> Périmètre technique : architecture monorepo / microservices event-driven. Le frontend Angular
> parle au seul **API Gateway** (REST + WebSocket), qui valide le JWT (JWKS de l'identity-service)
> et délègue l'autorisation à **OPA** (RBAC rôle × route au gateway, ABAC anti-IDOR côté services).
> Les microservices **ne s'appellent jamais en REST** : ils communiquent uniquement par **Kafka**
> et maintiennent chacun leurs **read-models locaux** (projections CQRS).

---

## 1. Rôles

Cinq rôles, chacun avec son profil d'accueil propre et un **accès documentaire filtré par rôle** :

| Rôle | Description | Périmètre |
|---|---|---|
| `admin` | Administrateur de la plateforme | Tous droits (super-utilisateur) |
| `administratif` | Personnel administratif | Documentation, budget, RH, communication |
| `enseignant` | Couvre Enseignants, Enseignants associés, Responsables de formation, Tuteurs | Formations, réunions, comptes rendus |
| `appui-insertion` | Personnel d'appui à l'insertion professionnelle | Suivi étudiant, stages, partenaires, statistiques d'insertion |
| `etudiant` | Étudiant inscrit | Son dossier, ses formations, ses documents et notifications |

---

## 2. Entités canoniques transverses

Pour éviter un modèle de données dupliqué, certaines entités sont **canoniques** : déclarées une
seule fois dans leur service propriétaire, puis **référencées par UUID** ailleurs (via projection
CQRS alimentée par Kafka).

| Entité canonique | Service propriétaire | Topic Kafka | Référencée par |
|---|---|---|---|
| **Étudiant** | people-service | `people.students` | Étudiant, Formations, Insertion, Communication |
| **Personnel / Formateur** | people-service | `people.staff` | Formations, Administration (RH), Communication |
| **Document** (+ archivage, accès par rôle) | document-service | `document.documents` | Communication, Administration |
| **Compte rendu / Réunion** | communication-service | `communication.comptesrendus`, `communication.reunions` | Communication, Formations |
| **Notification** | communication-service | `notifications` (+ WebSocket push) | Tous les modules |

> Clés primaires en **UUID** (anti-énumération / anti-IDOR). Stockage des fichiers binaires
> (logos, documents, courriers, comptes rendus, avatars) dans **MinIO** (S3).

---

## 3. Module Communication

**Service** : communication-service
**Topics** : `communication.comptesrendus`, `communication.reunions`, `notifications`
**Objet** : MinIO (`comptes-rendus`)

### Objectif
Centraliser la production et la diffusion des **comptes rendus** (réunions, séminaires, webinaires,
Conseil d'Université…), assurer l'**archivage documentaire filtré par rôle** et déclencher des
**notifications automatiques** (temps réel via WebSocket) à la publication d'un nouveau compte rendu
ou d'une circulaire.

### Entités
- **Réunion** : type (réunion, séminaire, webinaire, Conseil d'Université, tutorat…), date, lieu/lien,
  ordre du jour, organisateur (UUID personnel), participants (UUID).
- **CompteRendu** : réunion associée, titre, rédacteur, contenu, pièce jointe (MinIO), visibilité (liste de rôles), date de publication.
- **Notification** : destinataire(s) (UUID), type d'événement, libellé, ressource liée, statut lu/non lu, horodatage.

### Principaux cas d'usage
1. Planifier une réunion et inviter des participants.
2. Rédiger et publier un compte rendu, joindre un fichier (MinIO).
3. Consulter les comptes rendus **selon sa visibilité de rôle** (anti-IDOR : ABAC sur l'objet).
4. Recevoir une **notification temps réel** (push WebSocket) à la publication d'un compte rendu/circulaire.
5. Marquer une notification comme lue ; consulter l'historique.

### Rôles concernés
- `admin`, `administratif`, `enseignant` : créer/publier réunions et comptes rendus.
- Tous les rôles : recevoir les notifications et consulter les comptes rendus autorisés par leur visibilité.

---

## 4. Module Administration

**Service** : admin-service (budget) + document-service (gestion documentaire) + people-service (RH)
**Topics** : `admin.budget`, `document.documents`, `people.staff`, `people.students`
**Objet** : MinIO (`courriers`, `documents`)

### Objectif
Gérer la **documentation administrative** (courrier arrivé/départ, notes de service, circulaires), le
**budget** (projet de budget et budget réalisé) et les **ressources humaines** (dossiers du personnel
et des étudiants).

### Entités
- **Document administratif** : type (courrier arrivé, courrier départ, note de service, circulaire),
  référence, objet, expéditeur/destinataire, date, fichier (MinIO), visibilité par rôle, statut d'archivage.
- **LigneBudgetaire** / **Budget** : exercice, nature (projet de budget vs budget réalisé), poste,
  montant prévu, montant réalisé, écart.
- **DossierPersonnel** (projection de Personnel canonique) : identité, fonction, affectation, documents RH.
- **DossierEtudiant** (projection d'Étudiant canonique) : voir module Étudiant.

### Principaux cas d'usage
1. Enregistrer un courrier (arrivé/départ), une note de service ou une circulaire et l'archiver.
2. Consulter/rechercher les documents administratifs filtrés par rôle.
3. Saisir un **projet de budget** puis le **budget réalisé** ; comparer prévu/réalisé.
4. Exporter le budget et les statistiques (**PDF / Excel**).
5. Gérer les dossiers RH (personnel et étudiants) — référencés depuis people-service.

### Rôles concernés
- `admin`, `administratif` : gestion complète (documentation, budget, RH).
- `enseignant`, `appui-insertion`, `etudiant` : consultation des documents autorisés selon leur rôle.

---

## 5. Module Appui à l'insertion

**Service** : insertion-service
**Topics** : `insertion.events`, consomme `people.students`
**Objet** : MinIO (`documents`)

### Objectif
Assurer le **suivi des étudiants** vers l'emploi : registre de contact, **bilans de stages**,
**statistiques d'insertion** (auto-emploi vs emploi salarié) et **base de données des partenaires**.

### Entités
- **ContactSuivi** : étudiant (UUID, projection), date, canal, objet, compte rendu de l'échange.
- **Stage / BilanStage** : étudiant, partenaire/structure, période, sujet, évaluation, document (MinIO).
- **Partenaire** : raison sociale, secteur, contact, conventions, offres.
- **StatistiqueInsertion** (agrégat read-model) : par formation/promotion, répartition auto-emploi vs salarié, taux d'insertion.

### Principaux cas d'usage
1. Enregistrer un contact de suivi avec un étudiant (registre de contact).
2. Saisir et clôturer un **bilan de stage** (avec pièce jointe).
3. Gérer la **base partenaires** (entreprises, structures d'accueil).
4. Produire les **statistiques d'insertion** (auto-emploi vs emploi salarié) et les exporter (PDF/Excel).

### Rôles concernés
- `appui-insertion` : gestion complète du module.
- `admin` : tous droits ; `administratif` : consultation des statistiques.
- `etudiant` : consultation de son propre suivi et de ses bilans de stage (ABAC : propriétaire de l'objet).

---

## 6. Module Formations

**Service** : academic-service
**Topics** : `academic.formations`, consomme `people.staff`, `people.students`, `communication.reunions`

### Objectif
Décrire les **formations** (dates, type, niveau, financement, nombre de formés par genre), gérer les
**emplois du temps**, la **gestion des formateurs** et les **réunions** liées (tutorat, préparation de
cours, évaluations).

### Entités
- **Formation** : intitulé, type, niveau, période (début/fin), source de financement, effectifs par genre.
- **EmploiDuTemps / Seance** : formation, créneau, salle/lien, intervenant (UUID formateur), matière.
- **AffectationFormateur** : formation, formateur (UUID, projection de Personnel), rôle pédagogique.
- **Réunion pédagogique** (référence le module Communication) : tutorat, préparation de cours, évaluations.

### Principaux cas d'usage
1. Créer/éditer une formation (type, niveau, financement, effectifs par genre).
2. Construire et publier un **emploi du temps** (séances, intervenants, salles/liens).
3. **Affecter des formateurs** à une formation.
4. Planifier les **réunions pédagogiques** (tutorat / préparation cours / évaluations).
5. Consulter les statistiques de formation (effectifs par genre) et exporter (PDF/Excel).

### Rôles concernés
- `admin`, `administratif` : création/gestion des formations.
- `enseignant` (dont responsables de formation, tuteurs) : gestion pédagogique, emplois du temps, réunions.
- `etudiant` : consultation de **ses** formations et emplois du temps.

---

## 7. Module Étudiant

**Service** : people-service
**Topics** : `people.students`

### Objectif
Tenir le **dossier de l'étudiant** : identifiant (ID/INE), identité, formation, promotion, années de
début et de sortie, diplômes. C'est l'entité **canonique Étudiant** référencée par les autres modules.

### Entités
- **Étudiant** : UUID, INE, identité (nom, prénom, genre, date de naissance, contact), photo (MinIO `avatars`).
- **Inscription** : formation, promotion, année de début, année de sortie, statut.
- **Diplôme** : intitulé, niveau, date d'obtention, mention, document (MinIO).

### Principaux cas d'usage
1. Créer/mettre à jour le dossier d'un étudiant (identité, INE, contact).
2. Inscrire un étudiant à une formation/promotion ; renseigner années début/sortie.
3. Enregistrer les diplômes obtenus.
4. Consulter son propre dossier (étudiant) — accès au niveau objet (ABAC propriétaire).
5. Publier les évolutions du dossier sur `people.students` pour les projections des autres services.

### Rôles concernés
- `admin`, `administratif` : gestion complète des dossiers étudiants.
- `enseignant`, `appui-insertion` : consultation des dossiers des étudiants relevant de leur périmètre.
- `etudiant` : consultation/édition limitée de **son** dossier.

---

## 8. Matrice rôles × modules (accès)

Légende : **G** = Gestion complète (CRUD) · **C** = Consultation · **P** = Accès à ses propres objets
uniquement (ABAC propriétaire) · **—** = Aucun accès.

| Module \ Rôle | `admin` | `administratif` | `enseignant` | `appui-insertion` | `etudiant` |
|---|:---:|:---:|:---:|:---:|:---:|
| **Communication** | G | G | G | C | C (selon visibilité) |
| **Administration** | G | G | C | C | C (selon visibilité) |
| **Appui à l'insertion** | G | C | — | G | P |
| **Formations** | G | G | G | C | P (ses formations) |
| **Étudiant** | G | G | C | C | P (son dossier) |
| **Notifications** | C | C | C | C | C |

> Cette matrice exprime la **vue fonctionnelle**. La mise en œuvre technique combine :
> le **RBAC** au gateway (rôle × route, via OPA) et l'**ABAC** côté services (accès au niveau objet,
> via la garde anti-IDOR `sujet × action × ressource × attributs` dans `libs/common`). Politique
> *deny-by-default* : l'administrateur a tous les droits ; tout accès non explicitement autorisé est refusé.
