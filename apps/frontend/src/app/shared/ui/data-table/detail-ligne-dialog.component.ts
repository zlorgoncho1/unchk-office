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
 * Panneau latéral (drawer) générique de détail d'une ligne de tableau.
 * Rendu en colonne pleine hauteur : en-tête figé en haut (titre + bouton fermer),
 * contenu défilant au centre (les champs en une seule colonne aérée), actions en bas.
 * Affiche les valeurs de la ligne en paires libellé/valeur sans tronquer le contenu.
 */
@Component({
  selector: 'unchk-detail-ligne-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <!-- En-tête figé : titre à gauche, bouton fermer à droite. -->
    <div class="dld-entete">
      <h2 mat-dialog-title class="dld-titre">
        <iconify-icon icon="solar:document-text-bold-duotone"></iconify-icon>
        <span>{{ data.titre }}</span>
      </h2>
      <button
        mat-icon-button
        mat-dialog-close
        class="dld-fermer"
        aria-label="Fermer le panneau"
      >
        <iconify-icon icon="solar:close-circle-linear"></iconify-icon>
      </button>
    </div>

    <!-- Corps défilant : champs empilés en une seule colonne. -->
    <mat-dialog-content class="dld-corps">
      @for (c of data.champs; track c.libelle) {
        <div class="dld-champ">
          <span class="dld-libelle">{{ c.libelle }}</span>
          <span class="dld-valeur">{{ c.valeur }}</span>
        </div>
      }
    </mat-dialog-content>

    <!-- Actions figées en bas. -->
    <mat-dialog-actions align="end" class="dld-actions">
      <button mat-stroked-button mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      /* Colonne pleine hauteur : en-tête + contenu défilant + actions. */
      :host {
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      /* En-tête figé en haut. */
      .dld-entete {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 20px 24px 16px;
        border-bottom: 1px solid var(--unchk-border);
        flex: 0 0 auto;
      }
      .dld-titre {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        padding: 0;
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .dld-titre iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-blue);
      }
      .dld-fermer iconify-icon {
        font-size: 1.4rem;
        color: var(--unchk-text-muted);
      }

      /* Corps : une seule colonne aérée, défilant si trop haut. */
      .dld-corps {
        flex: 1 1 auto;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 16px;
        padding: 20px 24px !important;
        margin: 0;
        max-height: none;
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

      /* Actions figées en bas. */
      .dld-actions {
        flex: 0 0 auto;
        padding: 16px 24px 20px;
        border-top: 1px solid var(--unchk-border);
      }
    `,
  ],
})
export class DetailLigneDialog {
  readonly data = inject<DetailLigneData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<DetailLigneDialog>>(MatDialogRef);
}
