import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  input,
} from '@angular/core';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { SectionCardComponent } from '../section-card/section-card.component';

// Palette de marque réutilisée pour les graphes (cohérence avec _tokens.scss).
export const COULEURS_UNCHK = {
  blue: '#1C75BC',
  green: '#36A93B',
  orange: '#F39200',
  navy: '#16314A',
  slate: '#3E6E8E',
  blueLight: '#4F97D1',
  greenLight: '#5BC65F',
  orangeLight: '#FBA834',
} as const;

// Suite de couleurs pour les graphes catégoriels (camembert, barres multiples).
export const PALETTE_UNCHK: string[] = [
  COULEURS_UNCHK.blue,
  COULEURS_UNCHK.green,
  COULEURS_UNCHK.orange,
  COULEURS_UNCHK.slate,
  COULEURS_UNCHK.blueLight,
  COULEURS_UNCHK.greenLight,
];

/**
 * Carte de graphe brandée UNCHK.
 * Enveloppe une SectionCard autour d'un graphe Chart.js (via ng2-charts).
 * Le composant parent fournit le type, les données et, en option, les options.
 * Des options sobres par défaut sont appliquées (légende discrète, police Inter).
 */
@Component({
  selector: 'unchk-chart-card',
  standalone: true,
  imports: [SectionCardComponent, BaseChartDirective],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './chart-card.component.html',
  styleUrl: './chart-card.component.scss',
})
export class ChartCardComponent {
  // Titre de la carte (en-tête de section).
  readonly titre = input.required<string>();
  // Icône Solar optionnelle (sans le préfixe « solar: »).
  readonly icone = input<string | null>('chart-2-bold-duotone');
  // Type de graphe Chart.js (bar, line, doughnut, ...).
  readonly type = input.required<ChartType>();
  // Données du graphe (labels + datasets).
  readonly data = input.required<ChartData>();
  // Options spécifiques fournies par le parent (fusionnées avec les défauts).
  readonly options = input<ChartConfiguration['options']>();
  // Hauteur du graphe en pixels.
  readonly hauteur = input<number>(260);

  // Options finales : défauts sobres surchargés par celles du parent.
  readonly optionsFinales = computed<ChartConfiguration['options']>(() => {
    const cartesien = this.type() === 'bar' || this.type() === 'line';
    return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: {
          color: COULEURS_UNCHK.navy,
          font: { family: 'Inter, sans-serif', size: 12 },
          usePointStyle: true,
          boxWidth: 8,
          padding: 14,
        },
      },
      tooltip: {
        backgroundColor: COULEURS_UNCHK.navy,
        titleFont: { family: 'Inter, sans-serif' },
        bodyFont: { family: 'Inter, sans-serif' },
        padding: 10,
        cornerRadius: 6,
      },
    },
    // Axes en FRANÇAIS et compacts UNIQUEMENT pour les graphes cartésiens (bar/line) ;
    // jamais pour les doughnuts/pies (sinon des axes parasites apparaissent).
    ...(cartesien && { scales: {
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(22,49,74,0.06)' },
        ticks: {
          color: COULEURS_UNCHK.navy,
          font: { family: 'Inter, sans-serif', size: 11 },
          callback: (valeur) => {
            const n = Number(valeur);
            if (Math.abs(n) >= 1_000_000_000) {
              return `${(n / 1_000_000_000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Md`;
            }
            if (Math.abs(n) >= 1_000_000) {
              return `${(n / 1_000_000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} M`;
            }
            if (Math.abs(n) >= 1_000) {
              return `${(n / 1_000).toLocaleString('fr-FR', { maximumFractionDigits: 0 })} k`;
            }
            return n.toLocaleString('fr-FR');
          },
        },
      },
      x: {
        grid: { display: false },
        ticks: {
          color: COULEURS_UNCHK.navy,
          font: { family: 'Inter, sans-serif', size: 11 },
        },
      },
    } }),
    ...this.options(),
    };
  });
}
