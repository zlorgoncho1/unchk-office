import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { ChartData } from 'chart.js';

import { AuthService } from '../../../core/auth/auth.service';
import {
  AcademicService,
  Formation,
  InsertionService,
  PeopleService,
} from '../../../core/data';
import {
  ChartCardComponent,
  EmptyStateComponent,
  LoadingStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatCardComponent,
  StatusPillComponent,
} from '../../../shared/ui';
import { PALETTE_UNCHK } from '../../../shared/ui';
import { chargerDepuis } from '../../../shared/util/loadable';
import { formaterDate } from '../../../shared/util/format.util';

/**
 * Tableau de bord de l'étudiant.
 * Affiche sa fiche personnelle (/api/etudiants/me), un tableau de ses formations
 * (mat-table triable) et une visualisation de la répartition des effectifs formés.
 */
@Component({
  selector: 'app-etudiant-home',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatTableModule,
    PageHeaderComponent,
    StatCardComponent,
    SectionCardComponent,
    ChartCardComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    StatusPillComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './etudiant-home.component.html',
  styleUrl: '../home-shared.scss',
})
export class EtudiantHomeComponent {
  private readonly auth = inject(AuthService);
  private readonly people = inject(PeopleService);
  private readonly academic = inject(AcademicService);
  private readonly insertion = inject(InsertionService);

  // Prénom pour le message de bienvenue.
  readonly prenom = computed(() => {
    const u = this.auth.currentUser();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Bienvenue';
  });

  // Ressources chargées via le gateway.
  readonly fiche = chargerDepuis(() => this.people.maFiche());
  readonly formations = chargerDepuis(() => this.academic.listerFormations());

  readonly exposeUtil = formaterDate;

  // Colonnes du tableau des formations.
  readonly colonnesFormations = ['code', 'label', 'level', 'period'];

  // Formation de l'étudiant (mise en avant si on peut la résoudre).
  readonly maFormation = computed<Formation | null>(() => {
    const ref = this.fiche.etat().donnees?.formationRef;
    const liste = this.formations.etat().donnees ?? [];
    return ref ? (liste.find((f) => f.id === ref) ?? null) : null;
  });

  // Données du graphe : effectifs formés (hommes/femmes) sur les formations affichées.
  readonly grapheEffectifs = computed<ChartData<'bar'>>(() => {
    const liste = (this.formations.etat().donnees ?? []).slice(0, 6);
    return {
      labels: liste.map((f) => f.code),
      datasets: [
        {
          label: 'Hommes formés',
          data: liste.map((f) => f.trainedMale),
          backgroundColor: PALETTE_UNCHK[0],
          borderRadius: 4,
        },
        {
          label: 'Femmes formées',
          data: liste.map((f) => f.trainedFemale),
          backgroundColor: PALETTE_UNCHK[1],
          borderRadius: 4,
        },
      ],
    };
  });

  // Libellé du statut + ton sémantique pour la pastille.
  tonStatut(): 'info' | 'succes' | 'attention' | 'danger' | 'neutre' {
    const s = this.fiche.etat().donnees?.status;
    switch (s) {
      case 'inscrit':
        return 'info';
      case 'diplome':
        return 'succes';
      case 'suspendu':
        return 'attention';
      case 'abandon':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
