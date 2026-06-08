import { CUSTOM_ELEMENTS_SCHEMA, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { DiplomeDto } from '../../core/data';

/** Données passées au dialog d'édition des diplômes. */
export interface DiplomesDialogData {
  /** Nom complet de l'étudiant (affiché dans l'en-tête). */
  nomComplet: string;
  /** Diplômes existants (copiés à l'ouverture). */
  diplomes: DiplomeDto[];
}

/** Ligne éditable d'un diplôme (sous-ensemble saisi dans le formulaire). */
interface LigneDiplome {
  label: string;
  level: string;
  obtainedAt: string;
}

/**
 * Dialog dédié à l'édition de la liste des diplômes d'un étudiant.
 * <p>
 * Le FormDrawer générique ne sait pas gérer une liste imbriquée : on propose donc
 * un petit panneau latéral listant les diplômes (intitulé, niveau, date) avec ajout
 * et suppression. À la validation, renvoie le tableau de diplômes (prêt pour le PUT).
 * Annulation -> ferme sans valeur (undefined).
 */
@Component({
  selector: 'app-diplomes-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <!-- En-tête figé : titre + bouton fermer. -->
    <div class="dpl-entete">
      <h2 mat-dialog-title class="dpl-titre">
        <iconify-icon icon="solar:diploma-bold-duotone"></iconify-icon>
        <span>Diplômes — {{ data.nomComplet }}</span>
      </h2>
      <button
        mat-icon-button
        mat-dialog-close
        class="dpl-fermer"
        aria-label="Fermer le panneau"
      >
        <iconify-icon icon="solar:close-circle-linear"></iconify-icon>
      </button>
    </div>

    <!-- Corps défilant : une carte éditable par diplôme. -->
    <mat-dialog-content class="dpl-corps">
      @if (lignes().length === 0) {
        <p class="dpl-vide">Aucun diplôme. Ajoutez-en un ci-dessous.</p>
      }

      @for (d of lignes(); track $index) {
        <div class="dpl-carte">
          <div class="dpl-carte-entete">
            <span class="dpl-index">Diplôme {{ $index + 1 }}</span>
            <button
              mat-icon-button
              type="button"
              class="dpl-suppr"
              aria-label="Supprimer ce diplôme"
              (click)="supprimer($index)"
            >
              <iconify-icon icon="solar:trash-bin-trash-linear"></iconify-icon>
            </button>
          </div>

          <mat-form-field appearance="outline" class="dpl-champ">
            <mat-label>Intitulé</mat-label>
            <input matInput [(ngModel)]="d.label" name="label-{{ $index }}" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="dpl-champ">
            <mat-label>Niveau</mat-label>
            <input matInput [(ngModel)]="d.level" name="level-{{ $index }}" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="dpl-champ">
            <mat-label>Date d'obtention</mat-label>
            <input
              matInput
              type="date"
              [(ngModel)]="d.obtainedAt"
              name="date-{{ $index }}"
            />
          </mat-form-field>
        </div>
      }

      <!-- Ajoute une ligne de diplôme vierge. -->
      <button
        mat-stroked-button
        type="button"
        color="primary"
        class="dpl-ajouter"
        (click)="ajouter()"
      >
        <iconify-icon icon="solar:add-circle-linear"></iconify-icon>
        <span>&nbsp;Ajouter un diplôme</span>
      </button>
    </mat-dialog-content>

    <!-- Actions figées en bas. -->
    <mat-dialog-actions align="end" class="dpl-actions">
      <button mat-stroked-button (click)="annuler()">Annuler</button>
      <button mat-flat-button color="primary" (click)="valider()">
        Enregistrer
      </button>
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
      .dpl-entete {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 20px 22px 14px;
        border-bottom: 1px solid var(--unchk-border);
        flex: 0 0 auto;
      }
      .dpl-titre {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        padding: 0;
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .dpl-titre iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-blue);
      }
      .dpl-fermer iconify-icon {
        font-size: 1.4rem;
        color: var(--unchk-text-muted);
      }

      /* Corps : cartes empilées, défilant si trop haut. */
      .dpl-corps {
        flex: 1 1 auto;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 16px;
        padding: 20px 22px !important;
        margin: 0;
        max-height: none;
      }

      .dpl-vide {
        margin: 0;
        color: var(--unchk-text-muted);
        font-size: 0.9rem;
      }

      /* Carte d'un diplôme : entête + champs empilés. */
      .dpl-carte {
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 14px;
        border: 1px solid var(--unchk-border);
        border-radius: 10px;
        background: var(--unchk-surface);
      }
      .dpl-carte-entete {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      .dpl-index {
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--unchk-text-subtle);
      }
      .dpl-suppr iconify-icon {
        font-size: 1.25rem;
        color: var(--unchk-danger);
      }
      .dpl-champ {
        width: 100%;
      }

      .dpl-ajouter {
        align-self: flex-start;
      }

      /* Actions figées en bas. */
      .dpl-actions {
        flex: 0 0 auto;
        padding: 14px 22px 18px;
        border-top: 1px solid var(--unchk-border);
      }
    `,
  ],
})
export class DiplomesDialog {
  readonly data = inject<DiplomesDialogData>(MAT_DIALOG_DATA);
  private readonly ref =
    inject<MatDialogRef<DiplomesDialog, DiplomeDto[] | undefined>>(MatDialogRef);

  /** Lignes éditables initialisées depuis les diplômes existants (copie). */
  protected readonly lignes = signal<LigneDiplome[]>(
    (this.data.diplomes ?? []).map((d) => ({
      label: d.label ?? '',
      level: d.level ?? '',
      // On ne garde que la partie date (YYYY-MM-DD) pour l'input de type date.
      obtainedAt: (d.obtainedAt ?? '').slice(0, 10),
    }))
  );

  /** Ajoute une ligne de diplôme vierge en fin de liste. */
  protected ajouter(): void {
    this.lignes.update((l) => [...l, { label: '', level: '', obtainedAt: '' }]);
  }

  /** Supprime la ligne à l'index donné. */
  protected supprimer(index: number): void {
    this.lignes.update((l) => l.filter((_, i) => i !== index));
  }

  /**
   * Valide : ne conserve que les diplômes ayant un intitulé renseigné,
   * normalise les champs vides en null, puis ferme en renvoyant le tableau.
   * On n'envoie pas d'« id » (le backend recrée la liste à partir des entrées).
   */
  protected valider(): void {
    const diplomes = this.lignes()
      .filter((l) => l.label.trim() !== '')
      .map((l) => ({
        label: l.label.trim(),
        level: l.level.trim() || null,
        obtainedAt: l.obtainedAt || null,
      }));
    this.ref.close(diplomes as unknown as DiplomeDto[]);
  }

  protected annuler(): void {
    this.ref.close(undefined);
  }
}
