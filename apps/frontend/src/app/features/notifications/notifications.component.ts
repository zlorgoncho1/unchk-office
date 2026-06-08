import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';

import { AppNotification } from '../../core/models';
import { NotificationService } from '../../core/realtime/notification.service';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatCardComponent,
  StatusPillComponent,
  StatusPillTon,
} from '../../shared/ui';

/**
 * Page « Notifications ».
 * Affiche la liste des notifications temps réel reçues via le WebSocket
 * (titre/libellé, type, date, statut lu/non-lu) dans un tableau brandé.
 * La source est le signal du {@link NotificationService} : pas de chargement
 * HTTP, on lit directement l'état réactif déjà alimenté par le socket.
 */
@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    StatCardComponent,
    StatusPillComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export class NotificationsComponent {
  private readonly service = inject(NotificationService);

  // Source réactive : liste des notifications poussées par le service temps réel.
  protected readonly notifications = this.service.notifications;
  // Nombre de notifications non lues (badge / KPI).
  protected readonly nonLues = this.service.nonLues;
  // État de la connexion temps réel (deconnecte | connexion | connecte).
  protected readonly etat = this.service.etat;

  // Total de notifications affichées.
  protected readonly total = computed(() => this.notifications().length);
  // Nombre de notifications déjà lues.
  protected readonly lues = computed(
    () => this.total() - this.nonLues()
  );

  // Pastille d'état de la connexion temps réel (en-tête).
  protected readonly tonConnexion = computed<StatusPillTon>(() => {
    switch (this.etat()) {
      case 'connecte':
        return 'succes';
      case 'connexion':
        return 'attention';
      default:
        return 'danger';
    }
  });
  protected readonly libelleConnexion = computed(() => {
    switch (this.etat()) {
      case 'connecte':
        return 'Temps réel actif';
      case 'connexion':
        return 'Connexion…';
      default:
        return 'Hors ligne';
    }
  });

  // Colonnes du tableau des notifications.
  protected readonly colonnes: ColonneTable<AppNotification>[] = [
    {
      cle: 'libelle',
      libelle: 'Notification',
    },
    {
      cle: 'type',
      libelle: 'Type',
      type: 'pastille',
      ton: () => 'info',
    },
    {
      cle: 'horodatage',
      libelle: 'Date',
      type: 'date-heure',
      align: 'droite',
      largeur: '180px',
    },
    {
      cle: 'lu',
      libelle: 'Statut',
      type: 'pastille',
      align: 'centre',
      largeur: '120px',
      // On affiche un libellé lisible au lieu du booléen brut.
      valeur: (n) => (n.lu ? 'Lu' : 'Non lu'),
      ton: (n) => (n.lu ? 'neutre' : 'info'),
    },
  ];
}
