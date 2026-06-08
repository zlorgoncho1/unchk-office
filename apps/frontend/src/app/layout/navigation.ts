import { Role } from '../core/models';

// Un élément de navigation : route, libellé, icône Solar et rôles autorisés.
export interface NavItem {
  // Libellé affiché dans la barre latérale.
  libelle: string;
  // Chemin de routage (relatif à l'espace connecté).
  chemin: string;
  // Nom de l'icône Solar (sans le préfixe "solar:").
  icone: string;
  // Rôles autorisés à voir cet élément. Vide/absent = visible par tous.
  roles?: Role[];
}

// Un groupe d'éléments de navigation (section de la barre latérale).
export interface NavSection {
  // Titre de la section (peut être vide pour le groupe principal).
  titre: string;
  // Éléments de la section.
  elements: NavItem[];
}

/**
 * Définition de la navigation, groupée par sections.
 * Chaque élément déclare les rôles qui peuvent le voir ; la barre latérale
 * filtre selon le rôle de l'utilisateur courant (cf. matrice rôles × modules
 * de docs/specifications.md). L'admin voit tout.
 */
export const NAVIGATION: NavSection[] = [
  {
    titre: '',
    elements: [
      {
        libelle: 'Tableau de bord',
        chemin: 'accueil',
        icone: 'widget-5-bold-duotone',
        // Visible par tous les rôles.
      },
    ],
  },
  {
    titre: 'Pédagogie',
    elements: [
      {
        libelle: 'Formations',
        chemin: 'formations',
        icone: 'square-academic-cap-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant', 'etudiant'],
      },
      {
        libelle: 'Emplois du temps',
        chemin: 'emplois-du-temps',
        icone: 'calendar-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant', 'etudiant'],
      },
      {
        libelle: 'Étudiants',
        chemin: 'etudiants',
        icone: 'users-group-rounded-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant'],
      },
      {
        libelle: 'Mon dossier',
        chemin: 'mon-dossier',
        icone: 'user-id-bold-duotone',
        roles: ['etudiant'],
      },
    ],
  },
  {
    titre: 'Communication',
    elements: [
      {
        libelle: 'Réunions',
        chemin: 'reunions',
        icone: 'users-group-two-rounded-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant'],
      },
      {
        libelle: 'Comptes rendus',
        chemin: 'comptes-rendus',
        icone: 'document-text-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant'],
      },
      {
        libelle: 'Documents',
        chemin: 'documents',
        icone: 'folder-with-files-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant'],
      },
    ],
  },
  {
    titre: 'Insertion',
    elements: [
      {
        libelle: 'Suivi insertion',
        chemin: 'insertion',
        icone: 'case-round-minimalistic-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant', 'appui-insertion'],
      },
      {
        libelle: 'Partenaires',
        chemin: 'partenaires',
        icone: 'buildings-2-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant', 'appui-insertion'],
      },
      {
        libelle: 'Statistiques',
        chemin: 'statistiques',
        icone: 'chart-2-bold-duotone',
        roles: ['admin', 'administratif', 'enseignant', 'appui-insertion'],
      },
    ],
  },
  {
    titre: 'Administration',
    elements: [
      {
        libelle: 'Budgets',
        chemin: 'budgets',
        icone: 'wallet-money-bold-duotone',
        roles: ['admin', 'administratif'],
      },
      {
        libelle: 'Personnel',
        chemin: 'personnel',
        icone: 'users-group-rounded-bold-duotone',
        roles: ['admin', 'administratif'],
      },
    ],
  },
  {
    titre: 'Système',
    elements: [
      {
        libelle: 'Notifications',
        chemin: 'notifications',
        icone: 'bell-bold-duotone',
        // Visible par tous les rôles.
      },
      {
        libelle: 'Paramètres',
        chemin: 'parametres',
        icone: 'settings-bold-duotone',
        // Visible par tous les rôles.
      },
    ],
  },
];

/**
 * Filtre la navigation selon le rôle de l'utilisateur.
 * L'admin voit tout. Un élément sans `roles` est visible par tous.
 * Les sections vidées par le filtrage sont retirées.
 */
export function naviguerPourRoles(roles: Role[]): NavSection[] {
  const estAdmin = roles.includes('admin');
  return NAVIGATION.map((section) => ({
    titre: section.titre,
    elements: section.elements.filter(
      (el) =>
        estAdmin ||
        !el.roles ||
        el.roles.length === 0 ||
        el.roles.some((r) => roles.includes(r))
    ),
  })).filter((section) => section.elements.length > 0);
}
