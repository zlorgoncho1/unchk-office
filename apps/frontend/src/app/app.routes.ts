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
        data: { titre: 'Formations', icone: 'square-academic-cap-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'emplois-du-temps',
        title: 'Emplois du temps · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'etudiant')],
        data: { titre: 'Emplois du temps', icone: 'calendar-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'etudiants',
        title: 'Étudiants · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'enseignant', 'appui-insertion')],
        data: { titre: 'Étudiants', icone: 'users-group-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'mon-dossier',
        title: 'Mon dossier · UNCHK Office',
        canActivate: [roleGuard('admin', 'etudiant')],
        data: { titre: 'Mon dossier', icone: 'user-id-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },

      // --- Communication ---
      {
        path: 'reunions',
        title: 'Réunions · UNCHK Office',
        data: { titre: 'Réunions', icone: 'users-group-two-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'comptes-rendus',
        title: 'Comptes rendus · UNCHK Office',
        data: { titre: 'Comptes rendus', icone: 'document-text-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'documents',
        title: 'Documents · UNCHK Office',
        data: { titre: 'Documents', icone: 'folder-with-files-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },

      // --- Insertion ---
      {
        path: 'insertion',
        title: 'Suivi insertion · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'appui-insertion', 'etudiant')],
        data: { titre: 'Suivi insertion', icone: 'case-round-minimalistic-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'partenaires',
        title: 'Partenaires · UNCHK Office',
        canActivate: [roleGuard('admin', 'appui-insertion')],
        data: { titre: 'Partenaires', icone: 'buildings-2-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'statistiques',
        title: 'Statistiques · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif', 'appui-insertion')],
        data: { titre: "Statistiques d'insertion", icone: 'chart-2-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },

      // --- Administration ---
      {
        path: 'budgets',
        title: 'Budgets · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif')],
        data: { titre: 'Budgets', icone: 'wallet-money-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'personnel',
        title: 'Personnel · UNCHK Office',
        canActivate: [roleGuard('admin', 'administratif')],
        data: { titre: 'Personnel', icone: 'users-group-rounded-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },

      // --- Système ---
      {
        path: 'notifications',
        title: 'Notifications · UNCHK Office',
        data: { titre: 'Notifications', icone: 'bell-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },
      {
        path: 'parametres',
        title: 'Paramètres · UNCHK Office',
        data: { titre: 'Paramètres', icone: 'settings-bold-duotone' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then(
            (m) => m.PlaceholderComponent
          ),
      },

      // Racine de l'espace connecté -> tableau de bord.
      { path: '', pathMatch: 'full', redirectTo: 'accueil' },
    ],
  },

  // Tout chemin inconnu -> accueil (l'authGuard renverra vers /login si nécessaire).
  { path: '**', redirectTo: 'accueil' },
];
