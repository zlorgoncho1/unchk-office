import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, switchMap } from 'rxjs';

import {
  AdminService,
  BudgetDetail,
  BudgetResume,
  LigneBudgetaire,
} from '../../core/data';
import {
  ChampForm,
  ColonneTable,
  ConfirmDialog,
  DataTableComponent,
  FormDrawerComponent,
  optionsDrawer,
} from '../../shared/ui';
import { formaterMontant, humaniser } from '../../shared/util/format.util';

/** Données passées au drawer de gestion des lignes : le budget concerné. */
export interface LignesBudgetData {
  budget: BudgetResume;
}

/**
 * Drawer « Lignes du budget » : gère le budget réalisé d'un budget donné.
 * <p>
 * Ouvert en panneau latéral droit depuis la page Budgets (action « Détail »). Affiche
 * la table des lignes (poste, sens, prévu, réalisé, écart) avec :
 * <ul>
 *   <li>un bouton « Ajouter une ligne » (drawer de formulaire : poste, sens, prévu, libellé) ;</li>
 *   <li>par ligne, l'édition du montant réalisé (drawer mono-champ) ;</li>
 *   <li>par ligne, la suppression (confirmation) ;</li>
 *   <li>un pied de panneau récapitulant total prévu / réalisé / écart.</li>
 * </ul>
 * Le backend renvoie à chaque écriture le budget complet recalculé : on rafraîchit donc
 * l'état local depuis cette réponse, sans rappel supplémentaire. À la fermeture, on signale
 * à l'appelant si des modifications ont eu lieu pour qu'il recharge la liste des budgets.
 */
@Component({
  selector: 'app-lignes-budget-dialog',
  standalone: true,
  imports: [DataTableComponent, MatButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './lignes-budget-dialog.component.html',
  styleUrl: './lignes-budget-dialog.component.scss',
})
export class LignesBudgetDialog {
  private readonly admin = inject(AdminService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  protected readonly data = inject<LignesBudgetData>(MAT_DIALOG_DATA);
  // Renvoie true si au moins une écriture a eu lieu (pour recharger la liste parente).
  private readonly ref =
    inject<MatDialogRef<LignesBudgetDialog, boolean>>(MatDialogRef);

  // État local du budget (entête + lignes + totaux), rafraîchi à chaque écriture.
  protected readonly libelle = signal<string>('');
  protected readonly orientationNote = signal<string | null>(null);
  protected readonly lignes = signal<LigneBudgetaire[]>([]);
  protected readonly totalPrevu = signal<number>(0);
  protected readonly totalRealise = signal<number>(0);
  protected readonly chargement = signal<boolean>(true);
  protected readonly devise = signal<string>('XOF');

  // Indique si une modification a eu lieu (pilote la valeur de fermeture).
  private modifie = false;

  // Écart global = prévu − réalisé.
  protected readonly ecartGlobal = computed(
    () => this.totalPrevu() - this.totalRealise()
  );

  // Titre lisible du panneau (libellé courant + exercice).
  protected readonly titre = computed(
    () => `${this.libelle() || this.data.budget.label} — exercice ${this.data.budget.fiscalYear}`
  );

  // Expose le formatage des montants au template.
  protected readonly exposeMontant = (m: number | null): string =>
    formaterMontant(m, this.devise());

  // Colonnes de la table des lignes.
  protected readonly colonnes: ColonneTable<LigneBudgetaire>[] = [
    { cle: 'category', libelle: 'Poste' },
    {
      cle: 'direction',
      libelle: 'Sens',
      valeur: (l) => humaniser(l.direction),
      largeur: '110px',
    },
    { cle: 'plannedAmount', libelle: 'Prévu', type: 'montant', largeur: '140px' },
    { cle: 'realizedAmount', libelle: 'Réalisé', type: 'montant', largeur: '140px' },
    {
      cle: 'ecart',
      libelle: 'Écart',
      type: 'montant',
      valeur: (l) => Number(l.plannedAmount ?? 0) - Number(l.realizedAmount ?? 0),
      largeur: '140px',
    },
  ];

  constructor() {
    this.charger();
  }

  /** Charge le détail du budget (entête + lignes + totaux) au montage. */
  private charger(): void {
    this.chargement.set(true);
    this.admin.consulterBudget(this.data.budget.id).subscribe({
      next: (b) => this.appliquer(b),
      error: () => {
        this.chargement.set(false);
        this.snack.open('Lignes du budget indisponibles.', 'OK', { duration: 4000 });
      },
    });
  }

  /** Recopie l'état renvoyé par le backend dans les signaux locaux. */
  private appliquer(b: BudgetDetail): void {
    this.libelle.set(b.label ?? '');
    this.orientationNote.set(b.orientationNote ?? null);
    this.lignes.set(b.lignes ?? []);
    this.totalPrevu.set(Number(b.totalPlanned ?? 0));
    this.totalRealise.set(Number(b.totalRealized ?? 0));
    if (b.currency) {
      this.devise.set(b.currency);
    }
    this.chargement.set(false);
  }

  /** Ouvre le drawer d'ajout d'une ligne (poste, sens, montant prévu, libellé). */
  protected ajouter(): void {
    const champs: ChampForm[] = [
      { cle: 'category', libelle: 'Poste (catégorie)', requis: true, largeur: 'pleine' },
      {
        cle: 'direction',
        libelle: 'Sens',
        type: 'select',
        requis: true,
        options: [
          { valeur: 'depense', libelle: 'Dépense' },
          { valeur: 'recette', libelle: 'Recette' },
        ],
      },
      { cle: 'plannedAmount', libelle: 'Montant prévu', type: 'nombre', requis: true },
      { cle: 'label', libelle: 'Libellé', largeur: 'pleine' },
    ];
    this.ouvrirForm('Nouvelle ligne budgétaire', champs).subscribe((corps) => {
      if (corps) {
        this.ecrire(
          this.admin.ajouterLigneBudget(this.data.budget.id, corps),
          'Ligne ajoutée.'
        );
      }
    });
  }

  /** Ouvre le drawer de saisie du montant réalisé pour une ligne. */
  protected saisirRealise(l: LigneBudgetaire): void {
    const champs: ChampForm[] = [
      {
        cle: 'realizedAmount',
        libelle: 'Montant réalisé',
        type: 'nombre',
        requis: true,
        largeur: 'pleine',
        aide: `Poste « ${l.category} » — prévu : ${this.exposeMontant(l.plannedAmount)}`,
      },
    ];
    const initiale = { realizedAmount: l.realizedAmount };
    this.ouvrirForm('Saisir le réalisé', champs, initiale).subscribe((corps) => {
      if (corps) {
        this.ecrire(
          this.admin.majRealisationLigne(this.data.budget.id, l.id, corps),
          'Réalisé mis à jour.'
        );
      }
    });
  }

  /** Demande confirmation puis supprime une ligne. */
  protected supprimer(l: LigneBudgetaire): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer la ligne',
          message: `Confirmer la suppression du poste « ${l.category} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(
            this.admin.supprimerLigneBudget(this.data.budget.id, l.id),
            'Ligne supprimée.'
          );
        }
      });
  }

  /** Ouvre le drawer d'édition de l'entête (libellé, devise, note d'orientation). */
  protected modifierEntete(): void {
    const champs: ChampForm[] = [
      { cle: 'label', libelle: 'Libellé du budget', requis: true, largeur: 'pleine' },
      { cle: 'currency', libelle: 'Devise (ISO 4217, ex : XOF)' },
      {
        cle: 'orientationNote',
        libelle: "Note d'orientation",
        type: 'textarea',
        largeur: 'pleine',
      },
    ];
    const initiale: Record<string, unknown> = {
      label: this.libelle(),
      currency: this.devise(),
      orientationNote: this.orientationNote() ?? '',
    };
    this.ouvrirForm("Modifier l'entête", champs, initiale).subscribe((corps) => {
      if (corps) {
        // Le PUT renvoie le budget complet : on le relit pour rafraîchir l'état (entête inclus).
        const maj$ = this.admin
          .modifierBudget(this.data.budget.id, corps)
          .pipe(switchMap(() => this.admin.consulterBudget(this.data.budget.id)));
        this.ecrire(maj$, 'Entête mise à jour.');
      }
    });
  }

  /** Ferme le panneau en signalant si des modifications ont eu lieu. */
  protected fermer(): void {
    this.ref.close(this.modifie);
  }

  /** Ouvre un drawer de formulaire (au-dessus du panneau courant). */
  private ouvrirForm(
    titre: string,
    champs: ChampForm[],
    valeurInitiale?: Record<string, unknown>
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre, champs, valeurInitiale })
      )
      .afterClosed();
  }

  /**
   * Exécute une écriture (ligne ou entête) : la réponse contient le budget recalculé,
   * on rafraîchit l'état local depuis cette réponse et on notifie l'utilisateur.
   */
  private ecrire(source$: Observable<BudgetDetail>, messageOk: string): void {
    source$.subscribe({
      next: (b) => {
        this.modifie = true;
        this.appliquer(b);
        this.snack.open(messageOk, 'OK', { duration: 3000 });
      },
      error: () =>
        this.snack.open(
          'Action impossible (droits insuffisants ou données invalides).',
          'OK',
          { duration: 4000 }
        ),
    });
  }
}
