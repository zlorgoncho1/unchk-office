import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { CommunicationService, CompteRendu } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillTon,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';

/**
 * Page « Comptes rendus » : liste tabulaire des comptes rendus de la
 * communication (réunions, séminaires, conseils…). Source : gateway
 * /api/communication/comptes-rendus. Tableau brandé, filtrable.
 */
@Component({
  selector: 'app-comptes-rendus',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './comptes-rendus.component.html',
})
export class ComptesRendusComponent {
  private readonly svc = inject(CommunicationService);

  // Chargement réactif des comptes rendus (chargement / succès / erreur).
  protected readonly data = chargerDepuis(() => this.svc.listerComptesRendus());

  // Lignes du tableau (tableau vide tant que les données n'arrivent pas).
  protected readonly lignes = computed<CompteRendu[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<CompteRendu>[] = [
    { cle: 'title', libelle: 'Titre' },
    { cle: 'ownerName', libelle: 'Auteur' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      // Colonne courte (pastille) : largeur fixe pour ne pas voler l'espace au texte.
      largeur: '120px',
      ton: (c) => this.tonStatut(c.status),
    },
    // Colonne date courte : largeur fixe, les colonnes de texte long s'étirent.
    { cle: 'publishedAt', libelle: 'Publié le', type: 'date', largeur: '120px' },
    {
      cle: 'visibility',
      libelle: 'Visibilité',
      valeur: (c) => (c.visibility ?? []).join(', '),
    },
  ];

  // Ton sémantique de la pastille selon le statut du compte rendu.
  private tonStatut(statut: string): StatusPillTon {
    switch (statut) {
      case 'publie':
        return 'succes';
      case 'valide':
        return 'info';
      case 'brouillon':
        return 'attention';
      case 'archive':
        return 'neutre';
      default:
        return 'neutre';
    }
  }
}
