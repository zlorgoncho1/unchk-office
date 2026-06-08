import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
  output,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { LIBELLES_ROLES } from '../../core/models';
import { naviguerPourRoles } from '../navigation';

/**
 * Barre latérale de navigation.
 * - Logo horizontal en haut.
 * - Navigation groupée par sections, filtrée selon le rôle de l'utilisateur.
 * - Élément actif mis en valeur en pilule bleu primaire (via RouterLinkActive).
 * - Icônes Solar (web component Iconify).
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  private readonly auth = inject(AuthService);

  // Émis lorsqu'un lien est cliqué (pour refermer la sidebar en mobile).
  readonly naviguer = output<void>();

  // Utilisateur courant et son rôle principal (libellé affiché en pied).
  readonly user = this.auth.currentUser;
  readonly roleLibelle = computed(() => {
    const roles = this.user()?.roles ?? [];
    return roles.length ? LIBELLES_ROLES[roles[0]] : '';
  });

  // Sections de navigation filtrées par rôle (réactif au changement d'utilisateur).
  readonly sections = computed(() => naviguerPourRoles(this.user()?.roles ?? []));

  // Initiales pour l'avatar de pied de barre.
  readonly initiales = computed(() => {
    const u = this.user();
    const source = u?.fullName?.trim() || u?.email || '';
    const parts = source.split(/[\s@.]+/).filter(Boolean);
    return parts.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || 'U';
  });
}
