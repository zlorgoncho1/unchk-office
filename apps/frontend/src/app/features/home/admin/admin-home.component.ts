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
  AdminService,
  DocumentsService,
  PeopleService,
  Personnel,
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
 * Tableau de bord administrateur — vue d'ensemble de la plateforme.
 * Compteurs people / documents / formations, répartition des formations par
 * niveau (graphe) et annuaire du personnel (mat-table triable et filtrable).
 */
@Component({
  selector: 'app-admin-home',
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
  templateUrl: './admin-home.component.html',
  styleUrl: '../home-shared.scss',
})
export class AdminHomeComponent implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly people = inject(PeopleService);
  private readonly academic = inject(AcademicService);
  private readonly documents = inject(DocumentsService);
  private readonly admin = inject(AdminService);

  readonly exposeDate = formaterDate;
  readonly exposeHumaniser = humaniser;

  // Prénom pour le message de bienvenue.
  readonly prenom = computed(() => {
    const u = this.auth.currentUser();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Administrateur';
  });

  // Ressources : une page de chaque collection pour récupérer les compteurs.
  readonly etudiants = chargerDepuis(() => this.people.listerEtudiants(0, 1));
  readonly personnel = chargerDepuis(() => this.people.listerPersonnel(0, 50));
  readonly formations = chargerDepuis(() => this.academic.listerFormations());
  readonly docs = chargerDepuis(() => this.documents.lister(0, 1));
  readonly budgets = chargerDepuis(() => this.admin.listerBudgets());

  // Source de données de l'annuaire personnel (tri + filtre Material).
  readonly sourcePersonnel = new MatTableDataSource<Personnel>([]);
  readonly colonnesPersonnel = ['name', 'kind', 'department', 'grade', 'hiredAt'];

  @ViewChild(MatSort) sort?: MatSort;

  constructor() {
    // Branche la liste paginée du personnel sur la source du tableau dès réception.
    effect(() => {
      this.sourcePersonnel.data = this.personnel.etat().donnees?.content ?? [];
    });
  }

  ngAfterViewInit(): void {
    // Tri Material + accès aux champs imbriqués pour le tri par nom.
    if (this.sort) {
      this.sourcePersonnel.sort = this.sort;
    }
    this.sourcePersonnel.sortingDataAccessor = (item, prop) => {
      switch (prop) {
        case 'name':
          return `${item.lastName} ${item.firstName}`.toLowerCase();
        case 'hiredAt':
          return item.hiredAt ?? '';
        default:
          return (item[prop as keyof Personnel] as string) ?? '';
      }
    };
    // Filtre plein-texte sur nom/département/spécialité.
    this.sourcePersonnel.filterPredicate = (item, filtre) => {
      const cible = `${item.firstName} ${item.lastName} ${item.department ?? ''} ${item.speciality ?? ''} ${item.grade ?? ''}`.toLowerCase();
      return cible.includes(filtre.trim().toLowerCase());
    };
  }

  /** Applique le filtre saisi à la source du tableau. */
  filtrer(valeur: string): void {
    this.sourcePersonnel.filter = valeur;
  }

  // Compteurs dérivés.
  readonly nbEtudiants = computed(
    () => this.etudiants.etat().donnees?.totalElements ?? 0
  );
  readonly nbPersonnel = computed(
    () => this.personnel.etat().donnees?.totalElements ?? 0
  );
  readonly nbFormations = computed(
    () => this.formations.etat().donnees?.length ?? 0
  );
  readonly nbDocuments = computed(
    () => this.docs.etat().donnees?.totalElements ?? 0
  );

  // Graphe : répartition des formations par niveau (camembert).
  readonly grapheNiveaux = computed<ChartData<'doughnut'>>(() => {
    const liste = this.formations.etat().donnees ?? [];
    const parNiveau = new Map<string, number>();
    for (const f of liste) {
      const cle = humaniser(f.level);
      parNiveau.set(cle, (parNiveau.get(cle) ?? 0) + 1);
    }
    const labels = [...parNiveau.keys()];
    return {
      labels,
      datasets: [
        {
          data: labels.map((l) => parNiveau.get(l) ?? 0),
          backgroundColor: PALETTE_UNCHK,
          borderWidth: 0,
        },
      ],
    };
  });

  // Vrai si le tableau personnel est en cours de chargement.
  readonly chargementPersonnel = computed(
    () => this.personnel.etat().chargement
  );
  readonly erreurPersonnel = computed(() => this.personnel.etat().erreur);
  readonly aDuPersonnel = computed(
    () => (this.personnel.etat().donnees?.content?.length ?? 0) > 0
  );
}
