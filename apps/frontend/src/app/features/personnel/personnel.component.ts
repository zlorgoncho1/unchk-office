import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { PeopleService } from '../../core/data';
import { Personnel } from '../../core/data/api.models';
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
 * Page « Personnel » : annuaire du personnel + gestion complète (CRUD).
 * Liste filtrable (matricule, identité, catégorie, grade, département…), création /
 * modification (drawer de formulaire) et suppression (confirmation).
 * Suit le même schéma que la page Partenaires (page-header + bouton Nouveau,
 * data-table avec actions, form-drawer, confirm-dialog, rechargement).
 */
@Component({
  selector: 'app-personnel',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './personnel.component.html',
})
export class PersonnelComponent {
  private readonly people = inject(PeopleService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement de la première page (50 agents) via le gateway.
  protected readonly data = chargerDepuis(() => this.people.listerPersonnel(0, 50));

  // Lignes du tableau : contenu de la page (PageReponse<Personnel>).
  protected readonly lignes = computed<Personnel[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<Personnel>[] = [
    // Colonnes courtes : largeur bornée pour laisser respirer le texte long.
    { cle: 'matricule', libelle: 'Matricule', largeur: '120px' },
    { cle: 'lastName', libelle: 'Nom' },
    { cle: 'firstName', libelle: 'Prénom' },
    {
      cle: 'kind',
      libelle: 'Catégorie',
      type: 'pastille',
      largeur: '150px',
      // Catégorie humanisée (ex. « enseignant_associe » → « Enseignant associé »).
      valeur: (p) => humaniser(p.kind),
      ton: () => 'info',
    },
    { cle: 'grade', libelle: 'Grade' },
    { cle: 'department', libelle: 'Département' },
    { cle: 'speciality', libelle: 'Spécialité' },
    {
      cle: 'active',
      libelle: 'Statut',
      type: 'pastille',
      largeur: '110px',
      valeur: (p) => (p.active ? 'Actif' : 'Inactif'),
      ton: (p) => (p.active ? 'succes' : 'neutre'),
    },
  ];

  // Champs du formulaire = DTO CreerPersonnelRequest / ModifierPersonnelRequest.
  // Requis selon les @NotBlank/@NotNull du DTO : firstName, lastName, gender, kind.
  // (active est géré côté composant : true par défaut à la création, préservé à la modification.)
  private readonly champs: ChampForm[] = [
    { cle: 'firstName', libelle: 'Prénom', requis: true },
    { cle: 'lastName', libelle: 'Nom', requis: true },
    {
      cle: 'gender',
      libelle: 'Genre',
      type: 'select',
      requis: true,
      // Valeurs exactes de l'enum backend Genre.
      options: [
        { valeur: 'homme', libelle: 'Homme' },
        { valeur: 'femme', libelle: 'Femme' },
        { valeur: 'autre', libelle: 'Autre' },
      ],
    },
    {
      cle: 'kind',
      libelle: 'Catégorie',
      type: 'select',
      requis: true,
      // Valeurs exactes de l'enum backend StaffKind.
      options: [
        { valeur: 'enseignant', libelle: 'Enseignant' },
        { valeur: 'enseignant_associe', libelle: 'Enseignant associé' },
        { valeur: 'responsable_formation', libelle: 'Responsable de formation' },
        { valeur: 'tuteur', libelle: 'Tuteur' },
        { valeur: 'administratif', libelle: 'Administratif' },
        { valeur: 'appui_insertion', libelle: 'Appui à l’insertion' },
      ],
    },
    { cle: 'matricule', libelle: 'Matricule' },
    { cle: 'grade', libelle: 'Grade' },
    { cle: 'speciality', libelle: 'Spécialité' },
    { cle: 'department', libelle: 'Département' },
    { cle: 'email', libelle: 'Courriel', type: 'email' },
    { cle: 'phone', libelle: 'Téléphone', type: 'tel' },
    { cle: 'hiredAt', libelle: 'Date d’embauche', type: 'date' },
  ];

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau membre du personnel').subscribe((corps) => {
      if (corps) {
        // active:true par défaut (la liste ne renvoie que le personnel actif).
        this.ecrire(this.people.creerPersonnel({ ...corps, active: true }), 'Membre créé.');
      }
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli). */
  protected onModifier(p: Personnel): void {
    // Date d'embauche ISO -> yyyy-MM-dd pour pré-remplir l'<input type="date">.
    const initial: Personnel = {
      ...p,
      hiredAt: p.hiredAt ? p.hiredAt.slice(0, 10) : '',
    };
    this.ouvrirForm('Modifier le membre', initial).subscribe((corps) => {
      if (corps) {
        // On préserve l'état actif existant lors de la modification (champ requis).
        this.ecrire(
          this.people.modifierPersonnel(p.id, { ...corps, active: p.active }),
          'Membre modifié.'
        );
      }
    });
  }

  /** Demande confirmation puis supprime le membre du personnel. */
  protected onSupprimer(p: Personnel): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le membre',
          message: `Confirmer la suppression de « ${p.firstName} ${p.lastName} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.people.supprimerPersonnel(p.id), 'Membre supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    p?: Personnel
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({
          titre,
          champs: this.champs,
          valeurInitiale: p as unknown as Record<string, unknown>,
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
          'Action impossible (droits insuffisants ou données invalides).',
          'OK',
          { duration: 4000 }
        ),
    });
  }
}
