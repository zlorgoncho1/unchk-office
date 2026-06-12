import {
  AfterViewInit,
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  ViewChild,
  computed,
  effect,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { ChartData } from 'chart.js';

import { AuthService } from '../../../core/auth/auth.service';
import {
  AdminService,
  BudgetResume,
  Document,
  DocumentsService,
} from '../../../core/data';
import {
  ChartCardComponent,
  COULEURS_UNCHK,
  EmptyStateComponent,
  LoadingStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatCardComponent,
  StatusPillComponent,
} from '../../../shared/ui';
import { chargerDepuis } from '../../../shared/util/loadable';
import {
  formaterDate,
  formaterMontantCompact,
  pourcentage,
} from '../../../shared/util/format.util';

/**
 * Tableau de bord du personnel administratif.
 * Suivi documentaire (courriers/documents récents) et budgétaire : compteurs,
 * tableau des budgets triable/filtrable et graphe prévu vs réalisé.
 */
@Component({
  selector: 'app-administratif-home',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatTableModule,
    MatSortModule,
    PageHeaderComponent,
    StatCardComponent,
    SectionCardComponent,
    ChartCardComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    StatusPillComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './administratif-home.component.html',
  styleUrl: '../home-shared.scss',
})
export class AdministratifHomeComponent implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly documents = inject(DocumentsService);
  private readonly admin = inject(AdminService);

  readonly exposeDate = formaterDate;
  readonly exposeMontant = formaterMontantCompact;

  readonly prenom = computed(() => {
    const u = this.auth.currentUser();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Bienvenue';
  });

  // Ressources via le gateway.
  readonly docs = chargerDepuis(() => this.documents.lister(0, 20));
  readonly budgets = chargerDepuis(() => this.admin.listerBudgets());

  // Source du tableau des budgets (tri + filtre).
  readonly sourceBudgets = new MatTableDataSource<BudgetResume>([]);
  readonly colonnesBudgets = [
    'label',
    'fiscalYear',
    'status',
    'totalPlanned',
    'totalRealized',
    'taux',
  ];

  @ViewChild(MatSort) sort?: MatSort;

  constructor() {
    effect(() => {
      this.sourceBudgets.data = this.budgets.etat().donnees ?? [];
    });
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.sourceBudgets.sort = this.sort;
    }
    this.sourceBudgets.sortingDataAccessor = (item, prop) => {
      switch (prop) {
        case 'totalPlanned':
          return Number(item.totalPlanned ?? 0);
        case 'totalRealized':
          return Number(item.totalRealized ?? 0);
        case 'taux':
          return pourcentage(item.totalRealized, item.totalPlanned);
        case 'label':
          return (item.label ?? '').toLowerCase();
        default:
          return (item[prop as keyof BudgetResume] as string | number) ?? '';
      }
    };
    this.sourceBudgets.filterPredicate = (item, filtre) =>
      `${item.label} ${item.fiscalYear} ${item.status}`
        .toLowerCase()
        .includes(filtre.trim().toLowerCase());
  }

  filtrer(valeur: string): void {
    this.sourceBudgets.filter = valeur;
  }

  // Compteurs.
  readonly nbDocuments = computed(
    () => this.docs.etat().donnees?.totalElements ?? 0
  );
  readonly nbBudgets = computed(() => this.budgets.etat().donnees?.length ?? 0);

  // Total prévu / réalisé cumulés (tous budgets).
  readonly totalPrevu = computed(() =>
    (this.budgets.etat().donnees ?? []).reduce(
      (s, b) => s + Number(b.totalPlanned ?? 0),
      0
    )
  );
  readonly totalRealise = computed(() =>
    (this.budgets.etat().donnees ?? []).reduce(
      (s, b) => s + Number(b.totalRealized ?? 0),
      0
    )
  );
  readonly tauxEngagement = computed(() =>
    pourcentage(this.totalRealise(), this.totalPrevu())
  );

  // 5 documents les plus récents pour le panneau latéral.
  readonly documentsRecents = computed<Document[]>(() =>
    (this.docs.etat().donnees?.content ?? []).slice(0, 5)
  );

  // Graphe : prévu vs réalisé par budget (barres).
  readonly grapheBudgets = computed<ChartData<'bar'>>(() => {
    const liste = (this.budgets.etat().donnees ?? []).slice(0, 6);
    return {
      labels: liste.map((b) => b.label),
      datasets: [
        {
          label: 'Prévu',
          data: liste.map((b) => Number(b.totalPlanned ?? 0)),
          backgroundColor: COULEURS_UNCHK.blue,
          borderRadius: 4,
        },
        {
          label: 'Réalisé',
          data: liste.map((b) => Number(b.totalRealized ?? 0)),
          backgroundColor: COULEURS_UNCHK.green,
          borderRadius: 4,
        },
      ],
    };
  });

  // Pourcentage de réalisation d'un budget (cellule du tableau).
  taux(b: BudgetResume): number {
    return pourcentage(b.totalRealized, b.totalPlanned);
  }

  // Ton de la pastille selon le statut du budget.
  tonBudget(statut: string): 'info' | 'succes' | 'attention' | 'neutre' {
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
