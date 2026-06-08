import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AcademicService, Formation } from '../../core/data';
import {
  ChampForm,
  ColonneTable,
  ConfirmDialog,
  DataTableComponent,
  EmptyStateComponent,
  FormDrawerComponent,
  PageHeaderComponent,
  SectionCardComponent,
  optionsDrawer,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Formations » : liste tabulaire des formations de l'université + gestion complète (CRUD).
 * Données via AcademicService.listerFormations() (tableau brut).
 * Création / modification (drawer de formulaire) / suppression (confirmation),
 * sur le même schéma que la page Partenaires (référence du module).
 */
@Component({
  selector: 'app-formations',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
    DataTableComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './formations.component.html',
})
export class FormationsComponent {
  private readonly svc = inject(AcademicService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement des formations (le endpoint renvoie un tableau, pas une page).
  protected readonly data = chargerDepuis(() => this.svc.listerFormations());
  protected readonly lignes = computed<Formation[]>(
    () => this.data.etat().donnees ?? []
  );

  // Description des colonnes du tableau brandé.
  // Les colonnes courtes ont une largeur fixe pour laisser respirer
  // les colonnes de texte long (Intitulé, Type, Financement).
  protected readonly colonnes: ColonneTable<Formation>[] = [
    { cle: 'code', libelle: 'Code', largeur: '110px' },
    { cle: 'label', libelle: 'Intitulé' },
    {
      cle: 'level',
      libelle: 'Niveau',
      type: 'pastille',
      valeur: (f) => humaniser(f.level),
      ton: () => 'info',
      largeur: '120px',
    },
    { cle: 'kind', libelle: 'Type', valeur: (f) => humaniser(f.kind) },
    {
      cle: 'funding',
      libelle: 'Financement',
      valeur: (f) => humaniser(f.funding),
    },
    {
      // Montant du financement : colonne monétaire optionnelle (vide si non renseigné).
      cle: 'amount',
      libelle: 'Montant',
      type: 'montant',
      largeur: '130px',
    },
    {
      cle: 'formes',
      libelle: 'Formés',
      type: 'nombre',
      valeur: (f) => f.trainedMale + f.trainedFemale,
      largeur: '90px',
    },
    { cle: 'startDate', libelle: 'Début', type: 'date', largeur: '110px' },
    { cle: 'endDate', libelle: 'Fin', type: 'date', largeur: '110px' },
    {
      cle: 'active',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (f) => (f.active ? 'Active' : 'Inactive'),
      ton: (f) => (f.active ? 'succes' : 'neutre'),
      largeur: '110px',
    },
  ];

  // Champs du formulaire = DTO backend FormationCreationDto / FormationMajDto.
  // level (obligatoire), kind et funding sont des énumérations -> select.
  private readonly champs: ChampForm[] = [
    { cle: 'label', libelle: 'Intitulé de la formation', requis: true },
    { cle: 'code', libelle: 'Code' },
    {
      cle: 'level',
      libelle: 'Niveau',
      type: 'select',
      requis: true,
      options: [
        { valeur: 'certificat', libelle: 'Certificat' },
        { valeur: 'licence', libelle: 'Licence' },
        { valeur: 'master', libelle: 'Master' },
        { valeur: 'doctorat', libelle: 'Doctorat' },
        { valeur: 'formation_continue', libelle: 'Formation continue' },
      ],
    },
    {
      cle: 'kind',
      libelle: 'Type',
      type: 'select',
      options: [
        { valeur: 'initiale', libelle: 'Initiale' },
        { valeur: 'continue', libelle: 'Continue' },
        { valeur: 'professionnelle', libelle: 'Professionnelle' },
        { valeur: 'diplomante', libelle: 'Diplômante' },
        { valeur: 'qualifiante', libelle: 'Qualifiante' },
      ],
    },
    {
      cle: 'funding',
      libelle: 'Financement',
      type: 'select',
      options: [
        { valeur: 'etat', libelle: 'État' },
        { valeur: 'partenaire', libelle: 'Partenaire' },
        { valeur: 'autofinancement', libelle: 'Autofinancement' },
        { valeur: 'projet', libelle: 'Projet' },
        { valeur: 'mixte', libelle: 'Mixte' },
      ],
    },
    // Montant du financement (complète le type de financement, cf. énoncé).
    { cle: 'amount', libelle: 'Montant du financement', type: 'nombre' },
    { cle: 'startDate', libelle: 'Date de début', type: 'date' },
    { cle: 'endDate', libelle: 'Date de fin', type: 'date' },
    { cle: 'trainedMale', libelle: 'Formés (hommes)', type: 'nombre' },
    { cle: 'trainedFemale', libelle: 'Formés (femmes)', type: 'nombre' },
  ];

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouvelle formation').subscribe((corps) => {
      if (corps) {
        this.ecrire(this.svc.creerFormation(corps), 'Formation créée.');
      }
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli). */
  protected onModifier(f: Formation): void {
    this.ouvrirForm('Modifier la formation', f).subscribe((corps) => {
      if (corps) {
        // On préserve l'état actif existant de la formation lors de la modification.
        this.ecrire(
          this.svc.modifierFormation(f.id, { ...corps, active: f.active }),
          'Formation modifiée.'
        );
      }
    });
  }

  /** Demande confirmation puis supprime la formation. */
  protected onSupprimer(f: Formation): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer la formation',
          message: `Confirmer la suppression de « ${f.label} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimerFormation(f.id), 'Formation supprimée.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    f?: Formation
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({
          titre,
          champs: this.champs,
          valeurInitiale: f as unknown as Record<string, unknown>,
        })
      )
      .afterClosed();
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
          "Action impossible (droits insuffisants ou données invalides).",
          'OK',
          { duration: 4000 }
        ),
    });
  }
}
