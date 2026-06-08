import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, switchMap } from 'rxjs';

import { AdminService, BudgetResume, StatutBudget } from '../../core/data';
import {
  ChampForm,
  ColonneTable,
  ConfirmDialog,
  DataTableComponent,
  EmptyStateComponent,
  FormDrawerComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatCardComponent,
  StatusPillTon,
  optionsDrawer,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { formaterMontant, humaniser, pourcentage } from '../../shared/util/format.util';

/**
 * Page « Budgets » : liste des budgets de l'université + gestion complète (CRUD).
 * Compteurs en tête (nombre de budgets, total prévu, total réalisé) puis
 * tableau filtrable avec actions Modifier / Supprimer et drawer de formulaire.
 * Même schéma que la page Partenaires (page-header + bouton Nouveau, data-table,
 * form-drawer, confirm-dialog, rechargement).
 *
 * Particularité backend : le statut évolue via un endpoint dédié (PATCH /{id}/statut).
 * Le drawer expose un select « statut » et le composant déclenche le PATCH après
 * la création / modification quand le statut choisi diffère de l'existant.
 */
@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    StatCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.scss',
})
export class BudgetsComponent {
  private readonly admin = inject(AdminService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement réactif des budgets (chargement / succès / erreur).
  protected readonly data = chargerDepuis(() => this.admin.listerBudgets());

  // Lignes du tableau (tableau brut renvoyé par l'API).
  protected readonly lignes = computed<BudgetResume[]>(
    () => this.data.etat().donnees ?? []
  );

  // Compteurs synthétiques affichés dans les stat-cards.
  protected readonly nbBudgets = computed(() => this.lignes().length);
  protected readonly totalPrevu = computed(() =>
    this.lignes().reduce((s, b) => s + Number(b.totalPlanned ?? 0), 0)
  );
  protected readonly totalRealise = computed(() =>
    this.lignes().reduce((s, b) => s + Number(b.totalRealized ?? 0), 0)
  );

  // Expose le formatage de montant au template (stat-cards).
  protected readonly exposeMontant = formaterMontant;

  // Options du statut (= enum BudgetStatus côté backend).
  private readonly optionsStatut = [
    { valeur: 'projet', libelle: 'Projet' },
    { valeur: 'vote', libelle: 'Voté' },
    { valeur: 'en_execution', libelle: 'En exécution' },
    { valeur: 'cloture', libelle: 'Clôturé' },
  ];

  // Colonnes du tableau brandé.
  // Largeurs fixes sur les colonnes courtes : seul le libellé s'étire,
  // ce qui évite les retours à la ligne et équilibre la grille.
  protected readonly colonnes: ColonneTable<BudgetResume>[] = [
    { cle: 'label', libelle: 'Libellé' },
    {
      cle: 'fiscalYear',
      libelle: 'Exercice',
      type: 'nombre',
      align: 'centre',
      largeur: '100px',
    },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (b) => humaniser(b.status),
      ton: (b) => this.tonStatut(b.status),
      largeur: '140px',
    },
    { cle: 'totalPlanned', libelle: 'Prévu', type: 'montant', largeur: '170px' },
    { cle: 'totalRealized', libelle: 'Réalisé', type: 'montant', largeur: '170px' },
    {
      cle: 'taux',
      libelle: 'Taux %',
      type: 'nombre',
      valeur: (b) => pourcentage(b.totalRealized, b.totalPlanned),
      largeur: '90px',
    },
  ];

  /** Ouvre le drawer de création (l'exercice n'est saisissable qu'à la création). */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau budget').subscribe((corps) => {
      if (!corps) {
        return;
      }
      // Le statut suit un endpoint dédié : on l'extrait avant le POST (CreationBudgetDto
      // = fiscalYear, label, orientationNote, currency).
      const { status, ...entete } = corps;
      const cible = (status as StatutBudget) ?? 'projet';
      // Après création, on applique le statut choisi s'il diffère du défaut « projet ».
      const creation$ = this.admin.creerBudget(entete).pipe(
        switchMap((b) =>
          cible !== 'projet' ? this.admin.changerStatutBudget(b.id, cible) : of(b)
        )
      );
      this.ecrire(creation$, 'Budget créé.');
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli ; l'exercice n'est pas modifiable). */
  protected onModifier(b: BudgetResume): void {
    this.ouvrirForm('Modifier le budget', b).subscribe((corps) => {
      if (!corps) {
        return;
      }
      // MajBudgetDto = label, orientationNote, currency : on isole le statut.
      const { status, ...entete } = corps;
      const cible = status as StatutBudget | undefined;
      // PUT de l'entête, puis PATCH du statut seulement s'il a changé.
      const maj$ = this.admin.modifierBudget(b.id, entete).pipe(
        switchMap((maj) =>
          cible && cible !== b.status
            ? this.admin.changerStatutBudget(b.id, cible)
            : of(maj)
        )
      );
      this.ecrire(maj$, 'Budget modifié.');
    });
  }

  /** Demande confirmation puis supprime le budget. */
  protected onSupprimer(b: BudgetResume): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le budget',
          message: `Confirmer la suppression de « ${b.label} » (exercice ${b.fiscalYear}) ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.admin.supprimerBudget(b.id), 'Budget supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    b?: BudgetResume
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({
          titre,
          champs: this.champs(!b),
          valeurInitiale: b as unknown as Record<string, unknown>,
        })
      )
      .afterClosed();
  }

  /**
   * Champs du formulaire. L'exercice (fiscalYear) n'est présent qu'à la création :
   * le backend ne permet pas de le modifier (absent de MajBudgetDto).
   */
  private champs(creation: boolean): ChampForm[] {
    const exercice: ChampForm[] = creation
      ? [{ cle: 'fiscalYear', libelle: 'Exercice', type: 'nombre', requis: true }]
      : [];
    return [
      { cle: 'label', libelle: 'Libellé du budget', requis: true, largeur: 'pleine' },
      ...exercice,
      {
        cle: 'status',
        libelle: 'Statut',
        type: 'select',
        requis: true,
        options: this.optionsStatut,
      },
      { cle: 'currency', libelle: 'Devise (ISO 4217, ex : XOF)' },
      { cle: 'orientationNote', libelle: "Note d'orientation", type: 'textarea', largeur: 'pleine' },
    ];
  }

  /** Exécute une écriture, notifie et recharge la liste. */
  private ecrire(source$: Observable<unknown>, messageOk: string): void {
    source$.subscribe({
      next: () => {
        this.snack.open(messageOk, 'OK', { duration: 3000 });
        this.data.recharger();
      },
      error: () =>
        this.snack.open(
          'Action impossible (droits insuffisants ou données invalides).',
          'OK',
          { duration: 4000 }
        ),
    });
  }

  // Ton de la pastille selon le statut du budget.
  private tonStatut(statut: BudgetResume['status']): StatusPillTon {
    switch (statut) {
      case 'vote':
        return 'info';
      case 'en_execution':
        return 'attention';
      case 'cloture':
        return 'succes';
      default:
        return 'neutre';
    }
  }
}
