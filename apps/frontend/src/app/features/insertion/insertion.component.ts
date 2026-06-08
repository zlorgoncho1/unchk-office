import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { InsertionService, Stage, StatutStage } from '../../core/data';
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
 * Page « Suivi insertion » : liste des stages des étudiants.
 * Tableau brandé filtrable (intitulé, maître de stage, dates, statut, note).
 * Source : InsertionService.listerStages() -> Stage[].
 */
@Component({
  selector: 'app-insertion',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './insertion.component.html',
})
export class InsertionComponent {
  private readonly svc = inject(InsertionService);

  // Chargement des stages (état réactif : chargement / erreur / données).
  protected readonly data = chargerDepuis(() => this.svc.listerStages());

  // Lignes du tableau (liste simple, pas de pagination côté API).
  protected readonly lignes = computed<Stage[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau de suivi des stages.
  protected readonly colonnes: ColonneTable<Stage>[] = [
    { cle: 'title', libelle: 'Intitulé' },
    { cle: 'supervisorName', libelle: 'Maître de stage' },
    { cle: 'startDate', libelle: 'Début', type: 'date' },
    { cle: 'endDate', libelle: 'Fin', type: 'date' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      ton: (s) => this.tonStatut(s.status),
    },
    { cle: 'grade', libelle: 'Note', type: 'nombre' },
  ];

  // Ton de la pastille selon le statut du stage.
  private tonStatut(statut: StatutStage): StatusPillTon {
    switch (statut) {
      case 'prevu':
        return 'neutre';
      case 'en_cours':
        return 'attention';
      case 'termine':
        return 'info';
      case 'valide':
        return 'succes';
      case 'rompu':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
