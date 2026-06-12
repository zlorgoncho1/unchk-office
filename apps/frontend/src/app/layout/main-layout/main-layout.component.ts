import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/realtime/notification.service';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { RightRailComponent } from '../right-rail/right-rail.component';

/**
 * Conteneur principal de l'espace connecté.
 * Grille [sidebar | contenu (topbar + router-outlet) | rail droit].
 * Gère l'ouverture/fermeture de la sidebar et du rail en petit écran
 * et l'ouverture du canal de notifications temps réel.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, RightRailComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  // États mobile : panneaux en overlay.
  readonly menuOuvert = signal(false);
  readonly railOuvert = signal(false);
  // États desktop : repli pour gérer l'espace (sidebar en mini-icônes, rail masqué).
  readonly sidebarReplie = signal(false);
  // Rail droit (notifications/activités/contacts) MASQUÉ par défaut : il encombre les
  // pages de travail et les parcours. Le contenu gagne toute la largeur (tableaux et
  // graphiques tiennent sans débordement). La cloche de la topbar le rouvre au besoin.
  readonly railReplie = signal(true);

  ngOnInit(): void {
    // Ouvre le canal de notifications temps réel à l'entrée de l'espace connecté.
    this.notifications.connecter();
  }

  /** Bouton menu : replie la sidebar (desktop) ou ouvre l'overlay (mobile). */
  basculerMenu(): void {
    if (this.estPetitEcran(1024)) this.menuOuvert.update((v) => !v);
    else this.sidebarReplie.update((v) => !v);
  }

  /** Bouton cloche : masque le rail (desktop) ou ouvre l'overlay (mobile). */
  basculerRail(): void {
    if (this.estPetitEcran(1280)) this.railOuvert.update((v) => !v);
    else this.railReplie.update((v) => !v);
  }

  /** Vrai si la fenêtre est <= max (breakpoint tablette/mobile). */
  private estPetitEcran(max: number): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= max;
  }

  /** Ferme les deux panneaux (clic sur le voile ou navigation). */
  fermerPanneaux(): void {
    this.menuOuvert.set(false);
    this.railOuvert.set(false);
  }

  /** Déconnexion : ferme le temps réel, révoque la session, retour au login. */
  deconnexion(): void {
    this.notifications.deconnecter();
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
