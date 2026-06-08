import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  inject,
  output,
  signal,
} from '@angular/core';
import { Router, NavigationEnd, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/realtime/notification.service';
import { LIBELLES_ROLES } from '../../core/models';
import { NAVIGATION } from '../navigation';

// Un segment du fil d'Ariane.
interface FilSegment {
  libelle: string;
  chemin: string | null; // null = segment courant (non cliquable)
}

/**
 * Barre supérieure.
 * - Fil d'Ariane dérivé de la route courante.
 * - Champ de recherche.
 * - Icônes : thème, rafraîchir, langue et cloche de notifications (badge non lus).
 * - Avatar avec menu (profil, déconnexion).
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    RouterLink,
    MatMenuModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationService);

  // Émis pour ouvrir/fermer la barre latérale en mobile.
  readonly basculerMenu = output<void>();
  // Émis pour ouvrir/fermer le rail droit en mobile.
  readonly basculerRail = output<void>();
  // Émis pour demander une déconnexion (gérée par le layout parent).
  readonly deconnexion = output<void>();

  readonly user = this.auth.currentUser;
  readonly libellesRoles = LIBELLES_ROLES;

  // Fil d'Ariane courant.
  readonly fil = signal<FilSegment[]>([]);

  // Langue active (bascule purement visuelle pour l'instant).
  readonly langue = signal<'fr' | 'en'>('fr');

  constructor() {
    // Recalcule le fil d'Ariane à chaque navigation.
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((e) => this.fil.set(this.construireFil(e.urlAfterRedirects)));

    // Initialise le fil pour l'URL courante.
    this.fil.set(this.construireFil(this.router.url));
  }

  /** Rôle principal affiché (premier rôle). */
  get rolePrincipal(): string {
    const roles = this.user()?.roles ?? [];
    return roles.length ? LIBELLES_ROLES[roles[0]] : '';
  }

  /** Initiales pour l'avatar. */
  get initiales(): string {
    const u = this.user();
    const source = u?.fullName?.trim() || u?.email || '';
    const parts = source.split(/[\s@.]+/).filter(Boolean);
    return parts.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || 'U';
  }

  /** Recharge la vue courante (rafraîchissement). */
  rafraichir(): void {
    const url = this.router.url;
    void this.router
      .navigateByUrl('/', { skipLocationChange: true })
      .then(() => this.router.navigateByUrl(url));
  }

  /** Bascule la langue d'affichage (placeholder visuel). */
  basculerLangue(): void {
    this.langue.update((l) => (l === 'fr' ? 'en' : 'fr'));
  }

  // Construit le fil d'Ariane à partir de l'URL et de la table de navigation.
  private construireFil(url: string): FilSegment[] {
    const propre = url.split('?')[0].split('#')[0];
    const segment = propre.split('/').filter(Boolean)[0] ?? 'accueil';

    // Recherche le libellé dans la navigation déclarée.
    let libelle = 'Tableau de bord';
    for (const section of NAVIGATION) {
      const item = section.elements.find((el) => el.chemin === segment);
      if (item) {
        libelle = item.libelle;
        break;
      }
    }

    if (segment === 'accueil') {
      return [{ libelle: 'Tableau de bord', chemin: null }];
    }
    return [
      { libelle: 'Tableau de bord', chemin: '/accueil' },
      { libelle, chemin: null },
    ];
  }
}
