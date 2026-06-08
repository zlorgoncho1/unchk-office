import { CUSTOM_ELEMENTS_SCHEMA, Component, inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';

/** Données affichées par le dialog de détail d'une ligne. */
export interface DetailLigneData {
  titre: string;
  champs: { libelle: string; valeur: string }[];
}

/**
 * Dialog générique de détail d'une ligne de tableau.
 * Affiche les valeurs de la ligne en paires clé/valeur (style cohérent avec la charte).
 * Ouvert par {@code DataTableComponent} lorsque l'option « détaillable » est active.
 */
@Component({
  selector: 'unchk-detail-ligne-dialog',
  standalone: true,
  imports: [MatDialogModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <div class="dld">
      <header class="dld__entete">
        <iconify-icon icon="solar:document-text-bold-duotone"></iconify-icon>
        <h2 class="dld__titre">{{ data.titre }}</h2>
        <button type="button" class="dld__fermer" (click)="ref.close()" aria-label="Fermer">
          <iconify-icon icon="solar:close-circle-linear"></iconify-icon>
        </button>
      </header>
      <div class="dld__corps">
        @for (c of data.champs; track c.libelle) {
          <div class="dld__champ">
            <span class="dld__libelle">{{ c.libelle }}</span>
            <span class="dld__valeur">{{ c.valeur }}</span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .dld {
        min-width: 320px;
        max-width: 520px;
      }
      .dld__entete {
        display: flex;
        align-items: center;
        gap: 10px;
        padding-bottom: 14px;
        border-bottom: 1px solid var(--unchk-line);
        margin-bottom: 14px;
      }
      .dld__entete iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-blue);
      }
      .dld__titre {
        flex: 1;
        margin: 0;
        font-size: 1.05rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .dld__fermer {
        border: none;
        background: transparent;
        cursor: pointer;
        color: var(--unchk-text-subtle);
        display: inline-flex;
        font-size: 1.4rem;
      }
      .dld__fermer:hover {
        color: var(--unchk-danger);
      }
      .dld__corps {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 14px 24px;
      }
      .dld__champ {
        display: flex;
        flex-direction: column;
        gap: 2px;
      }
      .dld__libelle {
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--unchk-text-subtle);
      }
      .dld__valeur {
        font-size: 0.95rem;
        font-weight: 600;
        color: var(--unchk-navy);
        word-break: break-word;
      }
    `,
  ],
})
export class DetailLigneDialog {
  readonly data = inject<DetailLigneData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<DetailLigneDialog>>(MatDialogRef);
}
