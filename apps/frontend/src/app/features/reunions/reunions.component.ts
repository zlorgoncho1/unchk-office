import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { CommunicationService, Reunion } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillTon,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Réunions » : liste tabulaire des réunions de communication.
 * Source : CommunicationService.listerReunions() (tableau Reunion[]).
 * Tableau brandé filtrable avec pastilles de type et de statut.
 */
@Component({
  selector: 'app-reunions',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './reunions.component.html',
  styleUrl: '../home/home-shared.scss',
})
export class ReunionsComponent {
  private readonly svc = inject(CommunicationService);

  // Ressource réactive : la liste des réunions via le gateway.
  protected readonly data = chargerDepuis(() => this.svc.listerReunions());

  // Lignes du tableau (la source renvoie un simple tableau).
  protected readonly lignes = computed<Reunion[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau de réunions.
  protected readonly colonnes: ColonneTable<Reunion>[] = [
    // Colonne de texte long : laissée libre de s'étirer.
    { cle: 'title', libelle: 'Titre' },
    {
      cle: 'type',
      libelle: 'Type',
      type: 'pastille',
      valeur: (r) => humaniser(r.type),
      ton: () => 'info',
      largeur: '140px', // pastille courte : largeur fixe
    },
    { cle: 'startsAt', libelle: 'Début', type: 'date-heure', largeur: '150px' }, // date+heure : largeur fixe
    // Colonne de texte : laissée libre de s'étirer.
    { cle: 'location', libelle: 'Lieu' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (r) => humaniser(r.status),
      ton: (r) => this.tonStatut(r),
      largeur: '120px', // pastille courte : largeur fixe
    },
    // Colonne de texte : laissée libre de s'étirer.
    { cle: 'organizerName', libelle: 'Organisateur' },
  ];

  // Ton de la pastille selon le statut de la réunion.
  private tonStatut(r: Reunion): StatusPillTon {
    switch (r.status) {
      case 'planifiee':
        return 'info';
      case 'en_cours':
        return 'attention';
      case 'terminee':
        return 'succes';
      case 'annulee':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
