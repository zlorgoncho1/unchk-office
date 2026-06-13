import { CUSTOM_ELEMENTS_SCHEMA, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';

/** Données du dialog de confirmation. */
export interface ConfirmDialogData {
  titre: string;
  message: string;
  libelleConfirmer?: string;
  /** Style « danger » (rouge) pour une suppression. */
  danger?: boolean;
}

/**
 * Dialog de confirmation générique (oui / non). Renvoie {@code true} si confirmé.
 * Utilisé notamment avant une suppression.
 */
@Component({
  selector: 'unchk-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <h2 mat-dialog-title class="cd-titre">
      <iconify-icon
        [attr.icon]="
          data.danger
            ? 'solar:danger-triangle-bold-duotone'
            : 'solar:question-circle-bold-duotone'
        "
      ></iconify-icon>
      <span>{{ data.titre }}</span>
    </h2>
    <mat-dialog-content class="cd-message">{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button [mat-dialog-close]="false">Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [class.cd-confirm--danger]="data.danger"
        [mat-dialog-close]="true"
      >
        {{ data.libelleConfirmer || 'Confirmer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .cd-titre {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        font-size: 1.05rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .cd-titre iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-warning);
      }
      .cd-message {
        padding-top: 8px !important;
        color: var(--unchk-text);
        line-height: 1.5;
        max-width: 420px;
      }
      // Action destructive : bouton rouge (les tokens MDC traversent l'encapsulation).
      .cd-confirm--danger {
        --mdc-filled-button-container-color: var(--unchk-danger);
        --mdc-filled-button-label-text-color: #fff;
      }
    `,
  ],
})
export class ConfirmDialog {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<ConfirmDialog, boolean>>(MatDialogRef);
}
