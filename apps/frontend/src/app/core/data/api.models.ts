// ============================================================
// Modèles TypeScript miroir des DTO renvoyés par le gateway.
// Sources : controllers REST sous services/*/src/main/java.
// On ne déclare que les champs réellement consommés par les dashboards.
// ============================================================

// --- Pagination Spring (Page<T>) ---
// Le backend renvoie des objets Page paginés pour students, staff, documents.
export interface PageReponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // index de page (0-based)
  size: number;
}

// --- People : étudiant (/api/etudiants/me, /api/people/students) ---
export type StatutEtudiant = 'inscrit' | 'diplome' | 'abandon' | 'suspendu';
export type Genre = 'M' | 'F' | string;

export interface DiplomeDto {
  id: string;
  label: string;
  level: string;
  obtainedAt: string | null;
}

export interface Etudiant {
  id: string;
  ine: string | null;
  matricule: string | null;
  firstName: string;
  lastName: string;
  gender: Genre;
  birthDate: string | null;
  birthPlace: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  photoObjectKey: string | null;
  formationRef: string | null;
  promotion: string | null;
  enrollmentYear: number | null;
  exitYear: number | null;
  otherTrainings: string | null;
  status: StatutEtudiant;
  diplomas: DiplomeDto[];
  createdAt: string;
  updatedAt: string;
}

// --- People : personnel (/api/people/staff) ---
export type TypePersonnel = string;

export interface Personnel {
  id: string;
  matricule: string | null;
  firstName: string;
  lastName: string;
  gender: Genre;
  kind: TypePersonnel;
  email: string | null;
  phone: string | null;
  grade: string | null;
  speciality: string | null;
  department: string | null;
  photoObjectKey: string | null;
  active: boolean;
  hiredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// --- Academic : formation (/api/academic/formations) ---
export type NiveauFormation =
  | 'CERTIFICAT'
  | 'LICENCE'
  | 'MASTER'
  | 'DOCTORAT'
  | 'FORMATION_CONTINUE'
  | string;

export interface Formation {
  id: string;
  code: string;
  label: string;
  level: NiveauFormation;
  kind: string;
  funding: string;
  // Montant du financement (devise locale), optionnel.
  amount: number | null;
  startDate: string | null;
  endDate: string | null;
  trainedMale: number;
  trainedFemale: number;
  responsibleRef: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// --- Documents (/api/documents) ---
export interface Document {
  id: string;
  title: string;
  category: string | null;
  description: string | null;
  mimeType: string | null;
  sizeBytes: number;
  checksumSha256: string | null;
  ownerId: string | null;
  archived: boolean;
  sourceService: string | null;
  sourceRef: string | null;
  visibility: string[];
  createdAt: string;
  updatedAt: string;
}

// --- Communication : réunion (/api/communication/reunions) ---
export type StatutReunion = 'planifiee' | 'en_cours' | 'terminee' | 'annulee';
export type TypeReunion = string;

export interface ParticipantDto {
  userRef: string;
  displayName: string | null;
  status: string | null;
}

export interface Reunion {
  id: string;
  title: string;
  type: TypeReunion;
  description: string | null;
  location: string | null;
  startsAt: string;
  endsAt: string;
  status: StatutReunion;
  organizerId: string | null;
  organizerName: string | null;
  formationRef: string | null;
  participants: ParticipantDto[];
}

// --- Communication : compte rendu (/api/communication/comptes-rendus) ---
export interface CompteRendu {
  id: string;
  title: string;
  meetingRef: string | null;
  status: string;
  ownerId: string | null;
  ownerName: string | null;
  visibility: string[] | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// --- Communication : notification (/api/communication/notifications) ---
// Reflète NotificationDto côté backend (historique + marquage lu).
export interface NotificationApi {
  id: string;
  recipientId: string;
  kind: string;
  title: string;
  message: string | null;
  targetService: string | null;
  targetRef: string | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

// --- Admin : budget (/api/admin/budgets) ---
export type StatutBudget = 'projet' | 'vote' | 'en_execution' | 'cloture';

export interface BudgetResume {
  id: string;
  fiscalYear: number;
  label: string;
  status: StatutBudget;
  totalPlanned: number;
  totalRealized: number;
  currency: string;
}

// --- Insertion : stage (/api/insertion/stages) ---
export type StatutStage =
  | 'prevu'
  | 'en_cours'
  | 'termine'
  | 'rompu'
  | 'valide';

export interface Stage {
  id: string;
  studentRef: string;
  partnerId: string | null;
  title: string;
  startDate: string | null;
  endDate: string | null;
  status: StatutStage;
  tutorRef: string | null;
  supervisorName: string | null;
  reportRef: string | null;
  grade: number | null;
}

// --- Insertion : partenaire (/api/insertion/partenaires) ---
export type TypePartenaire =
  | 'entreprise'
  | 'administration'
  | 'ong'
  | 'institution'
  | 'autre';

export interface Partenaire {
  id: string;
  name: string;
  kind: TypePartenaire;
  sector: string | null;
  contactName: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  address: string | null;
  city: string | null;
  active: boolean;
}

// --- Insertion : registre de contact (/api/insertion/contacts) ---
export interface ContactRegistre {
  id: string;
  studentRef: string;
  contactedAt: string | null;
  channel: string | null;
  notes: string | null;
  agentRef: string | null;
}

// --- Insertion : situation d'insertion (/api/insertion/situations) ---
export type SituationInsertion =
  | 'emploi_salarie'
  | 'auto_emploi'
  | 'recherche_emploi'
  | 'poursuite_etudes'
  | 'sans_activite';

export interface SituationInsertionDto {
  id: string;
  studentRef: string;
  formationRef: string | null;
  kind: SituationInsertion;
  employerName: string | null;
  jobTitle: string | null;
  observedAt: string | null;
  current: boolean;
}

// --- Insertion : statistiques (/api/insertion/statistiques) ---
export interface StatistiqueFormation {
  formationRef: string | null;
  formationLabel: string;
  total: number;
  parType: Record<string, number>;
}

export interface StatistiquesInsertion {
  total: number;
  parType: Record<string, number>;
  parFormation: StatistiqueFormation[];
}
