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
  AcademicService,
  CommunicationService,
  Reunion,
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
import {
  formaterDateHeure,
  humaniser,
} from '../../../shared/util/format.util';

/**
 * Tableau de bord enseignant.
 * Met en avant les formations et les réunions à venir : compteurs, tableau des
 * réunions (mat-table triable/filtrable) et graphe des effectifs par formation.
 */
@Component({
  selector: 'app-enseignant-home',
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
  templateUrl: './enseignant-home.component.html',
  styleUrl: '../home-shared.scss',
})
export class EnseignantHomeComponent implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly academic = inject(AcademicService);
  private readonly communication = inject(CommunicationService);

  readonly exposeDateHeure = formaterDateHeure;
  readonly exposeHumaniser = humaniser;

  readonly prenom = computed(() => {
    const u = this.auth.currentUser();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Bienvenue';
  });

  // Ressources via le gateway.
  readonly formations = chargerDepuis(() => this.academic.listerFormations());
  readonly reunions = chargerDepuis(() => this.communication.listerReunions());

  // Source du tableau des réunions (tri + filtre).
  readonly sourceReunions = new MatTableDataSource<Reunion>([]);
  readonly colonnesReunions = ['title', 'type', 'startsAt', 'location', 'status'];

  @ViewChild(MatSort) sort?: MatSort;

  constructor() {
    effect(() => {
      this.sourceReunions.data = this.reunions.etat().donnees ?? [];
    });
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.sourceReunions.sort = this.sort;
    }
    this.sourceReunions.sortingDataAccessor = (item, prop) => {
      switch (prop) {
        case 'startsAt':
          return item.startsAt ?? '';
        case 'title':
          return (item.title ?? '').toLowerCase();
        default:
          return (item[prop as keyof Reunion] as string) ?? '';
      }
    };
    this.sourceReunions.filterPredicate = (item, filtre) =>
      `${item.title} ${item.type} ${item.location ?? ''} ${item.status}`
        .toLowerCase()
        .includes(filtre.trim().toLowerCase());
  }

  filtrer(valeur: string): void {
    this.sourceReunions.filter = valeur;
  }

  // Compteurs.
  readonly nbFormations = computed(
    () => this.formations.etat().donnees?.length ?? 0
  );
  readonly nbReunions = computed(() => this.reunions.etat().donnees?.length ?? 0);

  // Réunions à venir (planifiées avec une date future).
  readonly nbReunionsAVenir = computed(() => {
    const maintenant = Date.now();
    return (this.reunions.etat().donnees ?? []).filter(
      (r) => r.status === 'planifiee' && new Date(r.startsAt).getTime() >= maintenant
    ).length;
  });

  // Total d'apprenants formés (hommes + femmes) sur toutes les formations.
  readonly totalFormes = computed(() =>
    (this.formations.etat().donnees ?? []).reduce(
      (s, f) => s + (f.trainedMale ?? 0) + (f.trainedFemale ?? 0),
      0
    )
  );

  // Graphe : total formés par formation (barres horizontales sobres).
  readonly grapheFormations = computed<ChartData<'bar'>>(() => {
    const liste = (this.formations.etat().donnees ?? []).slice(0, 8);
    return {
      labels: liste.map((f) => f.code),
      datasets: [
        {
          label: 'Apprenants formés',
          data: liste.map((f) => (f.trainedMale ?? 0) + (f.trainedFemale ?? 0)),
          backgroundColor: PALETTE_UNCHK[0],
          borderRadius: 4,
        },
      ],
    };
  });

  // Ton de la pastille selon le statut de la réunion.
  tonReunion(statut: string): 'info' | 'succes' | 'attention' | 'danger' | 'neutre' {
    switch (statut) {
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
