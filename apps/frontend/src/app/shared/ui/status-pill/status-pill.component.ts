import { CUSTOM_ELEMENTS_SCHEMA, Component, input } from '@angular/core';

import { humaniser } from '../../util/format.util';

// Ton sémantique de la pastille de statut.
export type StatusPillTon = 'neutre' | 'info' | 'succes' | 'attention' | 'danger';

/**
 * Pastille de statut brandée UNCHK.
 * Affiche un libellé court coloré selon le ton sémantique. Utilisée dans
 * les tableaux des dashboards (statut d'étudiant, de budget, de réunion…).
 */
@Component({
  selector: 'unchk-status-pill',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './status-pill.component.html',
  styleUrl: './status-pill.component.scss',
})
export class StatusPillComponent {
  // Libellé brut (la valeur d'enum) ; humanisé à l'affichage si demandé.
  readonly libelle = input.required<string>();
  // Ton de couleur sémantique.
  readonly ton = input<StatusPillTon>('neutre');
  // Humanise le libellé (underscores -> espaces, capitale initiale).
  readonly humaniserLibelle = input<boolean>(true);

  // Texte affiché dans la pastille.
  texte(): string {
    return this.humaniserLibelle() ? humaniser(this.libelle()) : this.libelle();
  }
}
