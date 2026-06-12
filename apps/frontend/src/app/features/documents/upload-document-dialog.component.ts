import { CUSTOM_ELEMENTS_SCHEMA, Component, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { CATEGORIES_DOCUMENT } from './categories-document';

/** Résultat renvoyé à la fermeture du dialog : métadonnées + fichier à déposer. */
export interface UploadDocumentResultat {
  meta: {
    title: string;
    category: string;
    description?: string;
  };
  fichier: File;
}

/**
 * Dialog dédié au dépôt (upload) d'un document.
 * <p>
 * Le form-drawer générique ne sait pas gérer un champ fichier ; on fait donc un petit dialog
 * Material maison, ouvert en panneau latéral droit (via optionsDrawer côté appelant), qui
 * habille un formulaire réactif : titre (requis), catégorie (mat-select requis), description
 * (textarea) et un &lt;input type="file"&gt; requis. À la validation, on renvoie les métadonnées
 * et le fichier à l'appelant, qui se charge de l'appel multipart au service.
 */
@Component({
  selector: 'app-upload-document-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <!-- En-tête figé : titre + fermer. -->
    <div class="fd-entete">
      <h2 class="fd-titre">
        <iconify-icon icon="solar:upload-square-bold-duotone"></iconify-icon>
        <span>Nouveau document</span>
      </h2>
      <button
        mat-icon-button
        class="fd-fermer"
        type="button"
        (click)="annuler()"
        aria-label="Fermer"
      >
        <iconify-icon icon="solar:close-circle-linear"></iconify-icon>
      </button>
    </div>

    <!-- Corps défilant : formulaire réactif, une colonne. -->
    <form class="fd-corps" [formGroup]="form" (ngSubmit)="deposer()">
      <mat-form-field appearance="outline">
        <mat-label>Titre</mat-label>
        <input matInput type="text" formControlName="title" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Catégorie</mat-label>
        <mat-select formControlName="category">
          @for (c of categories; track c.valeur) {
            <mat-option [value]="c.valeur">{{ c.libelle }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Description</mat-label>
        <textarea matInput formControlName="description" rows="3"></textarea>
      </mat-form-field>

      <!-- Champ fichier maison (le form-field Material ne gère pas l'input file). -->
      <div class="ud-fichier">
        <span class="ud-fichier-libelle">Fichier *</span>
        <label class="ud-fichier-bouton">
          <iconify-icon icon="solar:upload-minimalistic-linear"></iconify-icon>
          <span>Choisir un fichier</span>
          <input
            type="file"
            (change)="onFichier($event)"
            aria-label="Choisir un fichier à déposer"
            hidden
          />
        </label>
        @if (fichier()) {
          <span class="ud-fichier-nom">{{ fichier()!.name }}</span>
        } @else {
          <span class="ud-fichier-vide">Aucun fichier sélectionné.</span>
        }
      </div>
    </form>

    <!-- Actions figées en bas. -->
    <div class="fd-actions">
      <button mat-stroked-button type="button" (click)="annuler()">Annuler</button>
      <button
        mat-flat-button
        color="primary"
        type="button"
        (click)="deposer()"
        [disabled]="form.invalid || !fichier()"
      >
        Déposer
      </button>
    </div>
  `,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .fd-entete {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 20px 22px 14px;
        border-bottom: 1px solid var(--unchk-border);
        flex: 0 0 auto;
      }
      .fd-titre {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--unchk-navy);
      }
      .fd-titre iconify-icon {
        font-size: 1.5rem;
        color: var(--unchk-blue);
      }
      .fd-fermer iconify-icon {
        font-size: 1.4rem;
        color: var(--unchk-text-muted);
      }
      .fd-corps {
        flex: 1 1 auto;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 2px;
        padding: 20px 22px 8px;
      }
      .fd-corps mat-form-field {
        width: 100%;
      }
      .ud-fichier {
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding: 4px 0 8px;
      }
      /* Bouton de sélection de fichier entièrement brandé (input natif masqué via [hidden]). */
      .ud-fichier-bouton {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        align-self: flex-start;
        padding: 9px 16px;
        border: 1px solid var(--unchk-blue);
        border-radius: var(--unchk-radius-md);
        background: rgba(28, 117, 188, 0.06);
        color: var(--unchk-blue);
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s ease;
      }
      .ud-fichier-bouton:hover {
        background: rgba(28, 117, 188, 0.12);
      }
      .ud-fichier-bouton iconify-icon {
        font-size: 1.15rem;
      }
      .ud-fichier-libelle {
        font-size: 0.85rem;
        font-weight: 600;
        color: var(--unchk-text);
      }
      .ud-fichier-nom {
        font-size: 0.85rem;
        color: var(--unchk-blue);
      }
      .ud-fichier-vide {
        font-size: 0.85rem;
        color: var(--unchk-text-muted);
      }
      .fd-actions {
        flex: 0 0 auto;
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        padding: 14px 22px 18px;
        border-top: 1px solid var(--unchk-border);
      }
    `,
  ],
})
export class UploadDocumentDialogComponent {
  private readonly ref =
    inject<MatDialogRef<UploadDocumentDialogComponent, UploadDocumentResultat | undefined>>(
      MatDialogRef
    );

  /** Catégories autorisées par le backend (title et category sont obligatoires). */
  protected readonly categories = CATEGORIES_DOCUMENT;

  /** Fichier choisi via l'input natif (signal pour piloter l'état du bouton). */
  protected readonly fichier = signal<File | null>(null);

  /** Formulaire réactif des métadonnées (le binaire est géré à part). */
  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    category: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
  });

  /** Mémorise le fichier sélectionné dans l'input natif. */
  protected onFichier(evt: Event): void {
    const cible = evt.target as HTMLInputElement;
    this.fichier.set(cible.files?.[0] ?? null);
  }

  /** Valide : renvoie les métadonnées (description optionnelle) + le fichier. */
  protected deposer(): void {
    const fichier = this.fichier();
    if (this.form.invalid || !fichier) {
      this.form.markAllAsTouched();
      return;
    }
    const brut = this.form.getRawValue();
    const meta: UploadDocumentResultat['meta'] = {
      title: brut.title,
      category: brut.category,
    };
    // La description ne part que si elle est renseignée.
    if (brut.description) {
      meta.description = brut.description;
    }
    this.ref.close({ meta, fichier });
  }

  protected annuler(): void {
    this.ref.close(undefined);
  }
}
