import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

// Table de routage de l'application.
// - /login : page publique de connexion.
// - Espace connecté : MainLayoutComponent (shell) en conteneur, protégé par authGuard,
//   avec les routes métier en enfants (router-outlet de la colonne centrale).
export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion · UNCHK Office',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent
      ),
  },

  // Conteneur de l'espace connecté (sidebar + topbar + rail droit).
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    children: [
      // --- Accueil : aiguillage vers le dashboard du rôle courant ---
      {
        path: 'accueil',
        title: 'Accueil · UNCHK Office',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/home/home-redirect.component').then(
            (m) => m.HomeRedirectComponent
          ),
      },
      {
        path: 'accueil/admin',
        title: "Vue d'ensemble · UNCHK Office",
        canActivate: [roleGuard('admin')],
        loadComponent: () =>
          import('./features/home/admin/admin-home.component').then(
            (m) => m.AdminHomeComponent
          ),
      },
      {
        path: 'accueil/administratif',
        title: 'Espace administratif · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif')],
        loadComponent: () =>
          import(
            './features/home/administratif/administratif-home.component'
          ).then((m) => m.AdministratifHomeComponent),
      },
      {
        path: 'accueil/enseignant',
        title: 'Espace enseignant · UNCHK Office',
        canActivate: [roleGuard('admin', 'enseignant')],
        loadComponent: () =>
          import('./features/home/enseignant/enseignant-home.component').then(
            (m) => m.EnseignantHomeComponent
          ),
      },
      {
        path: 'accueil/insertion',
        title: "Appui à l'insertion · UNCHK Office",
        canActivate: [roleGuard('admin', 'appui-insertion')],
        loadComponent: () =>
          import('./features/home/insertion/insertion-home.component').then(
            (m) => m.InsertionHomeComponent
          ),
      },
      {
        path: 'accueil/etudiant',
        title: 'Espace étudiant · UNCHK Office',
        canActivate: [roleGuard('admin', 'etudiant')],
        loadComponent: () =>
          import('./features/home/etudiant/etudiant-home.component').then(
            (m) => m.EtudiantHomeComponent
          ),
      },

      // --- Pédagogie ---
      {
        path: 'formations',
        title: 'Formations · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'etudiant')],
        data: { titre: 'Formations', icone: 'square-academic-cap-bold-duotone' },
        loadComponent: () =>
          import('./features/formations/formations.component').then(
            (m) => m.FormationsComponent
          ),
      },
      {
        path: 'emplois-du-temps',
        title: 'Emplois du temps · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'etudiant')],
        data: { titre: 'Emplois du temps', icone: 'calendar-bold-duotone' },
        loadComponent: () =>
          import('./features/emplois-du-temps/emplois-du-temps.component').then(
            (m) => m.EmploisDuTempsComponent
          ),
      },
      {
        path: 'etudiants',
        title: 'Étudiants · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant')],
        data: { titre: 'Étudiants', icone: 'users-group-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/etudiants/etudiants.component').then(
            (m) => m.EtudiantsComponent
          ),
      },
      {
        path: 'mon-dossier',
        title: 'Mon dossier · UNCHK Office',
        canActivate: [roleGuard('etudiant')],
        data: { titre: 'Mon dossier', icone: 'user-id-bold-duotone' },
        loadComponent: () =>
          import('./features/mon-dossier/mon-dossier.component').then(
            (m) => m.MonDossierComponent
          ),
      },

      // --- Communication ---
      {
        path: 'reunions',
        title: 'Réunions · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant')],
        data: { titre: 'Réunions', icone: 'users-group-two-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/reunions/reunions.component').then(
            (m) => m.ReunionsComponent
          ),
      },
      {
        path: 'comptes-rendus',
        title: 'Comptes rendus · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant')],
        data: { titre: 'Comptes rendus', icone: 'document-text-bold-duotone' },
        loadComponent: () =>
          import('./features/comptes-rendus/comptes-rendus.component').then(
            (m) => m.ComptesRendusComponent
          ),
      },
      {
        path: 'documents',
        title: 'Documents · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant')],
        data: { titre: 'Documents', icone: 'folder-with-files-bold-duotone' },
        loadComponent: () =>
          import('./features/documents/documents.component').then(
            (m) => m.DocumentsComponent
          ),
      },

      // --- Insertion ---
      {
        path: 'insertion',
        title: 'Suivi insertion · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'appui-insertion')],
        data: { titre: 'Suivi insertion', icone: 'case-round-minimalistic-bold-duotone' },
        loadComponent: () =>
          import('./features/insertion/insertion.component').then(
            (m) => m.InsertionComponent
          ),
      },
      {
        path: 'registre-contact',
        title: 'Registre de contact · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'appui-insertion')],
        data: { titre: 'Registre de contact', icone: 'users-group-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/registre-contact/registre-contact.component').then(
            (m) => m.RegistreContactComponent
          ),
      },
      {
        path: 'partenaires',
        title: 'Partenaires · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'appui-insertion')],
        data: { titre: 'Partenaires', icone: 'buildings-2-bold-duotone' },
        loadComponent: () =>
          import('./features/partenaires/partenaires.component').then(
            (m) => m.PartenairesComponent
          ),
      },
      {
        path: 'statistiques',
        title: 'Statistiques · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'appui-insertion')],
        data: { titre: "Statistiques d'insertion", icone: 'chart-2-bold-duotone' },
        loadComponent: () =>
          import('./features/statistiques/statistiques.component').then(
            (m) => m.StatistiquesComponent
          ),
      },

      // --- Administration ---
      {
        path: 'budgets',
        title: 'Budgets · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif')],
        data: { titre: 'Budgets', icone: 'wallet-money-bold-duotone' },
        loadComponent: () =>
          import('./features/budgets/budgets.component').then(
            (m) => m.BudgetsComponent
          ),
      },
      {
        path: 'personnel',
        title: 'Personnel · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif')],
        data: { titre: 'Personnel', icone: 'users-group-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/personnel/personnel.component').then(
            (m) => m.PersonnelComponent
          ),
      },

      // --- Système ---
      {
        path: 'notifications',
        title: 'Notifications · UNCHK Office',
        data: { titre: 'Notifications', icone: 'bell-bold-duotone' },
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(
            (m) => m.NotificationsComponent
          ),
      },
      {
        path: 'parametres',
        title: 'Paramètres · UNCHK Office',
        data: { titre: 'Paramètres', icone: 'settings-bold-duotone' },
        loadComponent: () =>
          import('./features/parametres/parametres.component').then(
            (m) => m.ParametresComponent
          ),
      },

      // Racine de l'espace connecté -> tableau de bord.
      { path: '', pathMatch: 'full', redirectTo: 'accueil' },
    ],
  },

  // Tout chemin inconnu -> accueil (l'authGuard renverra vers /login si nécessaire).
  { path: '**', redirectTo: 'accueil' },
];
