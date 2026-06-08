import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChartData, ChartOptions } from 'chart.js';
import { Observable } from 'rxjs';

import { AcademicService, InsertionService } from '../../core/data';
import { StatistiquesInsertion } from '../../core/data/api.models';
import {
  ChartCardComponent,
  COULEURS_UNCHK,
  EmptyStateComponent,
  LoadingStateComponent,
  PageHeaderComponent,
  PALETTE_UNCHK,
  SectionCardComponent,
  StatCardComponent,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { formaterNombre, humaniser } from '../../shared/util/format.util';
import { telechargerBlob } from '../../shared/util/telechargement.util';

/**
 * Page « Statistiques » d'insertion professionnelle.
 * KPIs (insérés, formations suivies, type dominant) et deux graphes :
 * répartition par type d'insertion (camembert) et insérés par formation (barres).
 * Source : InsertionService.statistiques() -> StatistiquesInsertion.
 */
@Component({
  selector: 'app-statistiques',
  standalone: true,
  imports: [
    PageHeaderComponent,
    StatCardComponent,
    SectionCardComponent,
    ChartCardComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './statistiques.component.html',
  styleUrl: './statistiques.component.scss',
})
export class StatistiquesComponent {
  private readonly svc = inject(InsertionService);
  private readonly academic = inject(AcademicService);
  private readonly snack = inject(MatSnackBar);

  // Chargement des statistiques (état réactif : chargement / erreur / données).
  protected readonly data = chargerDepuis(() => this.svc.statistiques());

  // Statistiques courantes (ou objet vide pour éviter les vérifications partout).
  private readonly stats = computed<StatistiquesInsertion>(
    () =>
      this.data.etat().donnees ?? { total: 0, parType: {}, parFormation: [] }
  );

  // --- KPIs dérivés -------------------------------------------------------

  // Nombre total d'étudiants insérés.
  protected readonly total = computed(() => this.stats().total);

  // Nombre de formations couvertes par les statistiques.
  protected readonly nbFormations = computed(
    () => this.stats().parFormation.length
  );

  // Nombre de types d'insertion distincts (auto-emploi, salarié…).
  protected readonly nbTypes = computed(
    () => Object.keys(this.stats().parType).length
  );

  // Type d'insertion dominant (libellé humanisé) ou tiret si aucune donnée.
  protected readonly typeDominant = computed(() => {
    const entrees = Object.entries(this.stats().parType);
    if (entrees.length === 0) {
      return '—';
    }
    const [cle] = entrees.reduce((max, cur) => (cur[1] > max[1] ? cur : max));
    return humaniser(cle);
  });

  // Vrai si au moins une donnée chiffrée est disponible.
  protected readonly aDesDonnees = computed(
    () => this.total() > 0 || this.nbFormations() > 0
  );

  // --- Graphe : répartition par type d'insertion (camembert) --------------

  protected readonly grapheTypes = computed<ChartData<'doughnut'>>(() => {
    const parType = this.stats().parType;
    const labels = Object.keys(parType).map((c) => humaniser(c));
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

  // --- Graphe : insérés par formation (barres) ----------------------------

  protected readonly grapheFormations = computed<ChartData<'bar'>>(() => {
    const formations = this.stats().parFormation;
    return {
      labels: formations.map((f) => f.formationLabel),
      datasets: [
        {
          label: 'Insérés',
          data: formations.map((f) => f.total),
          backgroundColor: COULEURS_UNCHK.blue,
          borderRadius: 6,
          maxBarThickness: 42,
        },
      ],
    };
  });

  // Options du graphe à barres : axe des valeurs en pas entiers, légende masquée.
  protected readonly optionsBarres: ChartOptions<'bar'> = {
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false } },
      y: { beginAtZero: true, ticks: { precision: 0 } },
    },
  };

  // Expose le formatage des nombres au template.
  protected readonly exposeNombre = formaterNombre;

  // --- Exports PDF / Excel ------------------------------------------------

  /** Exporte les statistiques d'insertion en PDF. */
  protected exporterInsertionPdf(): void {
    this.telecharger(
      this.svc.exporterStatistiquesPdf(),
      'statistiques-insertion.pdf'
    );
  }

  /** Exporte les statistiques d'insertion en Excel. */
  protected exporterInsertionExcel(): void {
    this.telecharger(
      this.svc.exporterStatistiquesExcel(),
      'statistiques-insertion.xlsx'
    );
  }

  /** Exporte les statistiques de formations (par genre) en PDF. */
  protected exporterFormationsPdf(): void {
    this.telecharger(
      this.academic.exporterFormationsPdf(),
      'statistiques-formations.pdf'
    );
  }

  /** Exporte les statistiques de formations (par genre) en Excel. */
  protected exporterFormationsExcel(): void {
    this.telecharger(
      this.academic.exporterFormationsXlsx(),
      'statistiques-formations.xlsx'
    );
  }

  /** Récupère le fichier binaire puis déclenche le téléchargement ; notifie en cas d'échec. */
  private telecharger(source$: Observable<Blob>, nomFichier: string): void {
    source$.subscribe({
      next: (blob) => telechargerBlob(blob, nomFichier),
      error: () =>
        this.snack.open(
          "Export impossible (droits insuffisants ou service indisponible).",
          'OK',
          { duration: 4000 }
        ),
    });
  }
}
