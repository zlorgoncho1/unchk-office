import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/models';

// Chemin du dashboard d'accueil par rôle.
const ACCUEIL_PAR_ROLE: Record<Role, string> = {
  admin: 'accueil/admin',
  administratif: 'accueil/administratif',
  enseignant: 'accueil/enseignant',
  'appui-insertion': 'accueil/insertion',
  etudiant: 'accueil/etudiant',
};

// Ordre de priorité si l'utilisateur cumule plusieurs rôles (le plus large gagne).
const PRIORITE: Role[] = [
  'admin',
  'administratif',
  'enseignant',
  'appui-insertion',
  'etudiant',
];

/**
 * Aiguilleur d'accueil.
 * Composant « vide » dont le seul rôle est de rediriger /accueil vers le
 * tableau de bord correspondant au rôle de l'utilisateur courant.
 */
@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: '',
})
export class HomeRedirectComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    const roles = this.auth.roles();
    // On choisit le rôle le plus prioritaire détenu par l'utilisateur.
    const role = PRIORITE.find((r) => roles.includes(r));
    const cible = role ? ACCUEIL_PAR_ROLE[role] : 'accueil/etudiant';
    // Remplacement dans l'historique : /accueil n'apparaît pas dans le « retour ».
    void this.router.navigate(['/', ...cible.split('/')], {
      replaceUrl: true,
    });
  }
}
