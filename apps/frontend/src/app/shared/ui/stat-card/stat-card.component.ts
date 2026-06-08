import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  input,
} from '@angular/core';

// Couleur de marque appliquée à l'accent de la carte.
export type StatCardTon = 'blue' | 'green' | 'orange' | 'navy';

/**
 * Carte de KPI brandée UNCHK.
 * Affiche un libellé, une valeur principale, une icône Solar et,
 * en option, une variation (tendance) avec un ton de couleur de marque.
 */
@Component({
  selector: 'unchk-stat-card',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  // Libellé du KPI (ex. « Étudiants suivis »).
  readonly libelle = input.required<string>();
  // Valeur principale affichée en gros.
  readonly valeur = input.required<string | number>();
  // Icône Solar (sans le préfixe « solar: »).
  readonly icone = input<string>('chart-2-bold-duotone');
  // Ton de couleur de marque pour l'icône et l'accent.
  readonly ton = input<StatCardTon>('blue');
  // Variation optionnelle (ex. « +12% ce mois »).
  readonly variation = input<string | null>(null);
  // Vrai si la variation est positive (vert), faux si négative (orange).
  readonly variationPositive = input<boolean>(true);
}
