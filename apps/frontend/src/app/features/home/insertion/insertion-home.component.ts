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
  InsertionService,
  Partenaire,
  Stage,
} from '../../../core/data';
import {
  ChartCardComponent,
  EmptyStateComponent,
  LoadingStateComponent,
  PageHeaderComponent,
  PALETTE_UNCHK,
  SectionCardComponent,
  StatCardComponent,
  StatusPillComponent,
} from '../../../shared/ui';
import { chargerDepuis } from '../../../shared/util/loadable';
import { formaterDate, humaniser } from '../../../shared/util/format.util';

/**
 * Tableau de bord « appui à l'insertion ».
 * Suivi des stages, des partenaires et des statistiques d'insertion : compteurs,
 * tableau des stages (mat-table triable/filtrable) et graphe de répartition des
 * situations d'insertion.
 */
@Component({
  selector: 'app-insertion-home',
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
  templateUrl: './insertion-home.component.html',
  styleUrl: '../home-shared.scss',
})
export class InsertionHomeComponent implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly insertion = inject(InsertionService);

  readonly exposeDate = formaterDate;
  readonly exposeHumaniser = humaniser;

  readonly prenom = computed(() => {
    const u = this.auth.currentUser();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Bienvenue';
  });

  // Ressources via le gateway.
  readonly stages = chargerDepuis(() => this.insertion.listerStages());
  readonly partenaires = chargerDepuis(() => this.insertion.listerPartenaires());
  readonly stats = chargerDepuis(() => this.insertion.statistiques());

  // Source du tableau des stages (tri + filtre).
  readonly sourceStages = new MatTableDataSource<Stage>([]);
  readonly colonnesStages = ['title', 'supervisorName', 'period', 'status', 'grade'];

  @ViewChild(MatSort) sort?: MatSort;

  constructor() {
    effect(() => {
      this.sourceStages.data = this.stages.etat().donnees ?? [];
    });
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.sourceStages.sort = this.sort;
    }
    this.sourceStages.sortingDataAccessor = (item, prop) => {
      switch (prop) {
        case 'period':
          return item.startDate ?? '';
        case 'grade':
          return Number(item.grade ?? 0);
        case 'title':
          return (item.title ?? '').toLowerCase();
        default:
          return (item[prop as keyof Stage] as string) ?? '';
      }
    };
    this.sourceStages.filterPredicate = (item, filtre) =>
      `${item.title} ${item.supervisorName ?? ''} ${item.status}`
        .toLowerCase()
        .includes(filtre.trim().toLowerCase());
  }

  filtrer(valeur: string): void {
    this.sourceStages.filter = valeur;
  }

  // Compteurs.
  readonly nbStages = computed(() => this.stages.etat().donnees?.length ?? 0);
  readonly nbPartenaires = computed(
    () => this.partenaires.etat().donnees?.length ?? 0
  );
  readonly totalInsertions = computed(() => this.stats.etat().donnees?.total ?? 0);

  // Stages en cours (statut en_cours).
  readonly nbStagesEnCours = computed(
    () =>
      (this.stages.etat().donnees ?? []).filter((s) => s.status === 'en_cours')
        .length
  );

  // Graphe : répartition des situations d'insertion par type (camembert).
  readonly grapheInsertion = computed<ChartData<'doughnut'>>(() => {
    const parType = this.stats.etat().donnees?.parType ?? {};
    const labels = Object.keys(parType).map((k) => humaniser(k));
    return {
      labels,
      datasets: [
        {
          data: Object.values(parType),
          backgroundColor: PALETTE_UNCHK,
          borderWidth: 0,
        },
      ],
    };
  });

  // Partenaires récents pour le panneau latéral.
  readonly partenairesRecents = computed<Partenaire[]>(() =>
    (this.partenaires.etat().donnees ?? []).slice(0, 5)
  );

  // Ton de la pastille selon le statut du stage.
  tonStage(statut: string): 'info' | 'succes' | 'attention' | 'danger' | 'neutre' {
    switch (statut) {
      case 'prevu':
        return 'neutre';
      case 'en_cours':
        return 'attention';
      case 'termine':
      case 'valide':
        return 'succes';
      case 'rompu':
        return 'danger';
      default:
        return 'info';
    }
  }
}
