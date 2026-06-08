import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';

import { AdminService, BudgetResume } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatCardComponent,
  StatusPillTon,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { formaterMontant, humaniser, pourcentage } from '../../shared/util/format.util';

/**
 * Page « Budgets » : liste des budgets de l'université via le tableau brandé.
 * Compteurs en tête (nombre de budgets, total prévu, total réalisé) puis
 * tableau filtrable (libellé, exercice, statut, prévu, réalisé, taux de réalisation).
 */
@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    StatCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.scss',
})
export class BudgetsComponent {
  private readonly admin = inject(AdminService);

  // Chargement réactif des budgets (chargement / succès / erreur).
  protected readonly data = chargerDepuis(() => this.admin.listerBudgets());

  // Lignes du tableau (tableau brut renvoyé par l'API).
  protected readonly lignes = computed<BudgetResume[]>(
    () => this.data.etat().donnees ?? []
  );

  // Compteurs synthétiques affichés dans les stat-cards.
  protected readonly nbBudgets = computed(() => this.lignes().length);
  protected readonly totalPrevu = computed(() =>
    this.lignes().reduce((s, b) => s + Number(b.totalPlanned ?? 0), 0)
  );
  protected readonly totalRealise = computed(() =>
    this.lignes().reduce((s, b) => s + Number(b.totalRealized ?? 0), 0)
  );

  // Expose le formatage de montant au template (stat-cards).
  protected readonly exposeMontant = formaterMontant;

  // Colonnes du tableau brandé.
  // Largeurs fixes sur les colonnes courtes : seul le libellé s'étire,
  // ce qui évite les retours à la ligne et équilibre la grille.
  protected readonly colonnes: ColonneTable<BudgetResume>[] = [
    { cle: 'label', libelle: 'Libellé' },
    {
      cle: 'fiscalYear',
      libelle: 'Exercice',
      type: 'nombre',
      align: 'centre',
      largeur: '100px',
    },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (b) => humaniser(b.status),
      ton: (b) => this.tonStatut(b.status),
      largeur: '140px',
    },
    { cle: 'totalPlanned', libelle: 'Prévu', type: 'montant', largeur: '170px' },
    { cle: 'totalRealized', libelle: 'Réalisé', type: 'montant', largeur: '170px' },
    {
      cle: 'taux',
      libelle: 'Taux %',
      type: 'nombre',
      valeur: (b) => pourcentage(b.totalRealized, b.totalPlanned),
      largeur: '90px',
    },
  ];

  // Ton de la pastille selon le statut du budget.
  private tonStatut(statut: BudgetResume['status']): StatusPillTon {
    switch (statut) {
      case 'vote':
        return 'info';
      case 'en_execution':
        return 'attention';
      case 'cloture':
        return 'succes';
      default:
        return 'neutre';
    }
  }
}
