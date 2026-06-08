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

  // États d'ouverture des panneaux latéraux (utiles en mobile).
  readonly menuOuvert = signal(false);
  readonly railOuvert = signal(false);

  ngOnInit(): void {
    // Ouvre le canal de notifications temps réel à l'entrée de l'espace connecté.
    this.notifications.connecter();
  }

  /** Ouvre/ferme la barre latérale (mobile). */
  basculerMenu(): void {
    this.menuOuvert.update((v) => !v);
  }

  /** Ouvre/ferme le rail droit (mobile). */
  basculerRail(): void {
    this.railOuvert.update((v) => !v);
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
