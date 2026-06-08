import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  booleanAttribute,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import {
  formaterDate,
  formaterDateHeure,
  formaterMontant,
  formaterNombre,
} from '../../util/format.util';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { LoadingStateComponent } from '../loading-state/loading-state.component';
import {
  StatusPillComponent,
  StatusPillTon,
} from '../status-pill/status-pill.component';
import { DetailLigneDialog } from './detail-ligne-dialog.component';

/** Type de rendu d'une colonne. */
export type TypeColonne =
  | 'texte'
  | 'nombre'
  | 'montant'
  | 'date'
  | 'date-heure'
  | 'pastille';

/**
 * Description d'une colonne de {@link DataTableComponent}.
 *
 * @typeParam T type d'une ligne de données
 */
export interface ColonneTable<T = Record<string, unknown>> {
  /** Clé de la propriété de la ligne (ou identifiant logique si {@link valeur} est fourni). */
  cle: string;
  /** Libellé affiché dans l'en-tête. */
  libelle: string;
  /** Type de rendu (défaut : texte). */
  type?: TypeColonne;
  /** Alignement (défaut : droite pour montant/nombre, gauche sinon). */
  align?: 'gauche' | 'droite' | 'centre';
  /** Accès personnalisé à la valeur (sinon {@code ligne[cle]}). */
  valeur?: (ligne: T) => unknown;
  /** Ton sémantique de la pastille (type=pastille). */
  ton?: (ligne: T) => StatusPillTon;
  /** Largeur CSS optionnelle (ex : « 120px »). */
  largeur?: string;
}

/**
 * Action de ligne d'un {@link DataTableComponent} (bouton icône en fin de ligne).
 *
 * @typeParam T type d'une ligne de données
 */
export interface ActionTable<T = Record<string, unknown>> {
  /** Identifiant de l'action (émis au clic). */
  cle: string;
  /** Libellé (infobulle + accessibilité). */
  libelle: string;
  /** Icône Solar (sans le préfixe « solar: »). */
  icone: string;
  /** Affiche l'action seulement si la condition est vraie pour la ligne. */
  visible?: (ligne: T) => boolean;
}

/**
 * Tableau de données brandé UNCHK, réutilisable sur toutes les pages.
 * <p>
 * Pilotage par configuration de colonnes ({@link ColonneTable}) : rendu homogène
 * (en-têtes marine en capitales, lignes survolables, montants tabulaires alignés à droite,
 * pastilles de statut). Gère le filtre texte, l'état de chargement et l'état vide, en
 * cohérence avec le design system (mêmes tokens, mêmes composants que les dashboards).
 */
@Component({
  selector: 'unchk-data-table',
  standalone: true,
  imports: [StatusPillComponent, EmptyStateComponent, LoadingStateComponent, MatDialogModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class DataTableComponent<T = Record<string, unknown>> {
  /** Colonnes à afficher. */
  readonly colonnes = input.required<ColonneTable<T>[]>();
  /** Lignes de données. */
  readonly lignes = input<readonly T[]>([]);
  /** Affiche l'état de chargement (spinner) à la place du tableau. */
  readonly chargement = input(false, { transform: booleanAttribute });
  /** Active la barre de filtre texte au-dessus du tableau (attribut nu accepté). */
  readonly filtrable = input(false, { transform: booleanAttribute });
  /** Texte indicatif du champ de filtre. */
  readonly placeholderFiltre = input<string>('Filtrer…');
  /** Titre de l'état vide. */
  readonly titreVide = input<string>('Aucune donnée');
  /** Message de l'état vide. */
  readonly messageVide = input<string | null>(null);
  /** Icône Solar de l'état vide. */
  readonly iconeVide = input<string>('inbox-line-duotone');
  /** Actions de ligne optionnelles : une colonne « Actions » est ajoutée à droite. */
  readonly actions = input<ActionTable<T>[]>([]);
  /** Émis au clic d'une action de ligne : {@code { action: clé, ligne }}. */
  readonly actionDeclenchee = output<{ action: string; ligne: T }>();
  /** Ajoute un bouton « Voir » par ligne ouvrant un dialog de détail générique. */
  readonly detaillable = input(false, { transform: booleanAttribute });

  private readonly dialog = inject(MatDialog);

  protected readonly filtre = signal('');

  /** Lignes après application du filtre texte (sur toutes les colonnes). */
  protected readonly lignesFiltrees = computed<readonly T[]>(() => {
    const q = this.filtre().trim().toLowerCase();
    const rows = this.lignes() ?? [];
    if (!q) {
      return rows;
    }
    const cols = this.colonnes();
    return rows.filter((ligne) =>
      cols.some((c) =>
        String(this.brut(c, ligne) ?? '')
          .toLowerCase()
          .includes(q)
      )
    );
  });

  /** Met à jour le filtre depuis l'événement de saisie. */
  protected majFiltre(evt: Event): void {
    this.filtre.set((evt.target as HTMLInputElement).value);
  }

  /** Valeur brute d'une cellule (avant formatage). */
  protected brut(col: ColonneTable<T>, ligne: T): unknown {
    return col.valeur ? col.valeur(ligne) : (ligne as Record<string, unknown>)[col.cle];
  }

  /** Valeur formatée d'une cellule selon le type de colonne. */
  protected afficher(col: ColonneTable<T>, ligne: T): string {
    const v = this.brut(col, ligne);
    switch (col.type) {
      case 'montant':
        return formaterMontant(v == null ? null : Number(v));
      case 'nombre':
        return formaterNombre(v == null ? null : Number(v));
      case 'date':
        return formaterDate(v as string);
      case 'date-heure':
        return formaterDateHeure(v as string);
      default:
        return v == null || v === '' ? '—' : String(v);
    }
  }

  /** Ton de la pastille pour une ligne. */
  protected ton(col: ColonneTable<T>, ligne: T): StatusPillTon {
    return col.ton ? col.ton(ligne) : 'neutre';
  }

  /** Classe d'alignement de la cellule (droite par défaut pour les nombres/montants). */
  protected classe(col: ColonneTable<T>): string {
    const align =
      col.align ?? (col.type === 'montant' || col.type === 'nombre' ? 'droite' : 'gauche');
    return align === 'droite' ? 'dt--droite' : align === 'centre' ? 'dt--centre' : '';
  }

  /** Indique si une colonne d'actions doit être affichée. */
  protected aDesActions(): boolean {
    return this.detaillable() || this.actions().length > 0;
  }

  /** Ouvre le dialog de détail générique avec les valeurs formatées de la ligne. */
  protected ouvrirDetail(ligne: T): void {
    const champs = this.colonnes().map((c) => ({
      libelle: c.libelle,
      valeur: this.afficher(c, ligne),
    }));
    // Ouverture en panneau latéral (drawer) collé au bord droit de l'écran.
    this.dialog.open(DetailLigneDialog, {
      data: { titre: 'Détail', champs },
      autoFocus: false,
      position: { top: '0', right: '0' },
      height: '100vh',
      width: '440px',
      maxWidth: '95vw',
      panelClass: 'unchk-drawer',
    });
  }

  /** Actions visibles pour une ligne donnée. */
  protected actionsVisibles(ligne: T): ActionTable<T>[] {
    return this.actions().filter((a) => !a.visible || a.visible(ligne));
  }

  /** Émet l'action déclenchée pour la ligne (sans propager le clic à la ligne). */
  protected declencher(action: ActionTable<T>, ligne: T, evt: Event): void {
    evt.stopPropagation();
    this.actionDeclenchee.emit({ action: action.cle, ligne });
  }
}
