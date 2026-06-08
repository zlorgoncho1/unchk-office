import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';

import { NotificationService } from '../../core/realtime/notification.service';
import { AppNotification } from '../../core/models';
import { EmptyStateComponent } from '../../shared/ui';

// Activité récente (placeholder ; sera alimentée par les services métier).
interface ActiviteRecente {
  icone: string;
  ton: 'blue' | 'green' | 'orange';
  texte: string;
  quand: string;
}

// Contact (placeholder ; sera alimenté par people-service).
interface Contact {
  nom: string;
  role: string;
  initiales: string;
  enLigne: boolean;
}

/**
 * Rail droit.
 * - Notifications temps réel (NotificationService / WebSocket).
 * - Activités récentes.
 * - Contacts.
 */
@Component({
  selector: 'app-right-rail',
  standalone: true,
  imports: [EmptyStateComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './right-rail.component.html',
  styleUrl: './right-rail.component.scss',
})
export class RightRailComponent {
  private readonly notifService = inject(NotificationService);

  // Notifications temps réel (les 6 plus récentes pour le rail).
  readonly notifications = computed(() => this.notifService.notifications().slice(0, 6));
  readonly nonLues = this.notifService.nonLues;
  readonly etat = this.notifService.etat;

  // Activités récentes (placeholder en attendant les flux métier).
  readonly activites: ActiviteRecente[] = [
    {
      icone: 'document-add-bold-duotone',
      ton: 'blue',
      texte: 'Nouveau compte rendu publié',
      quand: 'Il y a 2 h',
    },
    {
      icone: 'calendar-mark-bold-duotone',
      ton: 'green',
      texte: 'Réunion pédagogique planifiée',
      quand: 'Hier',
    },
    {
      icone: 'folder-with-files-bold-duotone',
      ton: 'orange',
      texte: 'Circulaire ajoutée à la documentation',
      quand: 'Il y a 2 j',
    },
  ];

  // Contacts (placeholder en attendant people-service).
  readonly contacts: Contact[] = [
    { nom: 'Service scolarité', role: 'Administratif', initiales: 'SS', enLigne: true },
    { nom: 'Appui insertion', role: 'Insertion', initiales: 'AI', enLigne: true },
    { nom: 'Support technique', role: 'Système', initiales: 'ST', enLigne: false },
  ];

  /** Marque une notification comme lue. */
  marquerLue(n: AppNotification): void {
    if (!n.lu) {
      this.notifService.marquerLue(n.id);
    }
  }

  /** Formate un horodatage ISO en libellé relatif simple (fr). */
  tempsRelatif(iso: string): string {
    const date = new Date(iso).getTime();
    if (Number.isNaN(date)) {
      return '';
    }
    const diff = Math.round((Date.now() - date) / 1000);
    if (diff < 60) return "À l'instant";
    if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`;
    return `Il y a ${Math.floor(diff / 86400)} j`;
  }
}
