import { CUSTOM_ELEMENTS_SCHEMA, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
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
 * Affiche les valeurs de la ligne en paires clé/valeur. S'appuie sur la structure
 * Material (titre / contenu défilant / actions) pour un espacement correct et la
 * gestion du débordement (le contenu défile si nécessaire, sans être tronqué).
 */
@Component({
  selector: 'unchk-detail-ligne-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <h2 mat-dialog-title class="dld-titre">
      <iconify-icon icon="solar:document-text-bold-duotone"></iconify-icon>
      <span>{{ data.titre }}</span>
    </h2>

    <mat-dialog-content class="dld-corps">
      @for (c of data.champs; track c.libelle) {
        <div class="dld-champ">
          <span class="dld-libelle">{{ c.libelle }}</span>
          <span class="dld-valeur">{{ c.valeur }}</span>
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dld-titre {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        padding-bottom: 4px;
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .dld-titre iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-blue);
      }

      /* Contenu : grille aérée 1–2 colonnes, défilante si trop haute. */
      .dld-corps {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 20px 28px;
        padding-top: 12px !important;
        padding-bottom: 4px !important;
        max-height: 60vh;
      }

      .dld-champ {
        display: flex;
        flex-direction: column;
        gap: 5px;
        min-width: 0;
      }
      .dld-libelle {
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--unchk-text-subtle);
      }
      .dld-valeur {
        font-size: 0.98rem;
        font-weight: 600;
        color: var(--unchk-navy);
        line-height: 1.4;
        word-break: break-word;
      }
    `,
  ],
})
export class DetailLigneDialog {
  readonly data = inject<DetailLigneData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<DetailLigneDialog>>(MatDialogRef);
}
