import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { LIBELLES_ROLES, Role } from '../../core/models';
import {
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillComponent,
} from '../../shared/ui';

/**
 * Page « Paramètres ».
 * Affiche en lecture seule le profil du compte connecté (nom, courriel, rôles)
 * reconstruit depuis les claims du JWT via AuthService, ainsi qu'un bloc de
 * préférences statiques (langue, thème). Aucun appel réseau.
 */
@Component({
  selector: 'app-parametres',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    StatusPillComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './parametres.component.html',
  styleUrl: './parametres.component.scss',
})
export class ParametresComponent {
  private readonly auth = inject(AuthService);

  // Utilisateur courant (null si la session a expiré entre-temps).
  protected readonly utilisateur = this.auth.currentUser;

  // Nom complet affichable (repli sur le courriel si absent).
  protected readonly nomAffiche = computed(() => {
    const u = this.utilisateur();
    return u?.fullName?.trim() || u?.email || '—';
  });

  // Rôles de l'utilisateur courant.
  protected readonly roles = computed<Role[]>(() => this.utilisateur()?.roles ?? []);

  // Libellé français d'un rôle (pour la pastille).
  protected libelleRole(role: Role): string {
    return LIBELLES_ROLES[role] ?? role;
  }
}
