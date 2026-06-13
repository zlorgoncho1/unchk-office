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
import {
  MAT_DATE_LOCALE,
  MatNativeDateModule,
} from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';

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
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  // Dates en français (jj/mm/aaaa) pour le sélecteur de date de TOUS les formulaires.
  providers: [{ provide: MAT_DATE_LOCALE, useValue: 'fr-FR' }],
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
      const brut = this.data.valeurInitiale?.[champ.cle] ?? '';
      // Les champs date utilisent un MatDatepicker (objet Date) : on convertit la
      // valeur initiale ISO « yyyy-MM-dd » en Date (ou null si absente).
      const valeur =
        champ.type === 'date' ? (brut ? this.versDate(String(brut)) : null) : brut;
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
      // Date (du datepicker) -> chaîne ISO « yyyy-MM-dd » attendue par le backend.
      const v = val instanceof Date ? this.versIso(val) : val;
      // On n'envoie pas les chaînes vides (laisse le backend appliquer ses défauts).
      if (v !== '' && v !== null && v !== undefined) {
        valeurs[cle] = v;
      }
    }
    this.ref.close(valeurs);
  }

  /** « yyyy-MM-dd » (ou ISO complet) -> Date locale (minuit, sans décalage de fuseau). */
  private versDate(iso: string): Date | null {
    const jour = iso.slice(0, 10);
    const d = new Date(`${jour}T00:00:00`);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  /** Date -> « yyyy-MM-dd » (composantes locales, pas d'UTC pour éviter le décalage). */
  private versIso(d: Date): string {
    const p = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  }

  protected annuler(): void {
    this.ref.close(undefined);
  }
}
