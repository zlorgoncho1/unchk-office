import { CUSTOM_ELEMENTS_SCHEMA, Component, inject } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

/** Type d'un champ de formulaire. */
export type TypeChamp =
  | 'texte'
  | 'email'
  | 'tel'
  | 'nombre'
  | 'date'
  | 'textarea'
  | 'select';

/** Description d'un champ du {@link FormDrawerComponent}. */
export interface ChampForm {
  /** Nom du champ = nom exact attendu par le DTO backend (ex : « firstName »). */
  cle: string;
  /** Libellé affiché. */
  libelle: string;
  /** Type de champ (défaut : texte). */
  type?: TypeChamp;
  /** Champ obligatoire. */
  requis?: boolean;
  /** Options pour un champ select : { valeur, libelle }. */
  options?: { valeur: string; libelle: string }[];
  /** Texte indicatif. */
  placeholder?: string;
  /** Largeur sur 2 colonnes : 'pleine' occupe toute la ligne (défaut : demi). */
  largeur?: 'demi' | 'pleine';
  /** Indice/aide sous le champ. */
  aide?: string;
}

/** Données passées au drawer de formulaire. */
export interface FormDrawerData {
  /** Titre du drawer (ex : « Nouvel étudiant »). */
  titre: string;
  /** Champs du formulaire. */
  champs: ChampForm[];
  /** Valeurs initiales (mode édition) ; vide pour une création. */
  valeurInitiale?: Record<string, unknown>;
  /** Libellé du bouton de validation (défaut : « Enregistrer »). */
  libelleValider?: string;
}

/**
 * Drawer de formulaire générique (création / édition), piloté par configuration.
 * <p>
 * Rendu en panneau latéral droit (même habillage que le drawer de détail) : en-tête figé,
 * formulaire défilant (champs Material sur une grille 2 colonnes), actions figées en bas.
 * À la validation, ferme le dialog en renvoyant l'objet des valeurs (prêt pour le POST/PUT).
 * Annulation -> ferme sans valeur (undefined).
 */
@Component({
  selector: 'unchk-form-drawer',
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
  templateUrl: './form-drawer.component.html',
  styleUrl: './form-drawer.component.scss',
})
export class FormDrawerComponent {
  readonly data = inject<FormDrawerData>(MAT_DIALOG_DATA);
  private readonly ref =
    inject<MatDialogRef<FormDrawerComponent, Record<string, unknown> | undefined>>(MatDialogRef);

  /** Formulaire réactif construit à partir des champs. */
  protected readonly form = new FormGroup<Record<string, FormControl>>({});

  constructor() {
    for (const champ of this.data.champs) {
      const valeur = this.data.valeurInitiale?.[champ.cle] ?? '';
      this.form.addControl(
        champ.cle,
        new FormControl(valeur, champ.requis ? Validators.required : []),
      );
    }
  }

  /** Valide : renvoie l'objet des valeurs non vides (nettoyées). */
  protected valider(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const brut = this.form.getRawValue();
    const valeurs: Record<string, unknown> = {};
    for (const [cle, val] of Object.entries(brut)) {
      // On n'envoie pas les chaînes vides (laisse le backend appliquer ses défauts).
      if (val !== '' && val !== null && val !== undefined) {
        valeurs[cle] = val;
      }
    }
    this.ref.close(valeurs);
  }

  protected annuler(): void {
    this.ref.close(undefined);
  }
}
