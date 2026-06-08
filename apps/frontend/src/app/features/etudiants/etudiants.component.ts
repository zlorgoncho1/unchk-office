import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { Etudiant, PeopleService } from '../../core/data';
import {
  ChampForm,
  ColonneTable,
  ConfirmDialog,
  DataTableComponent,
  EmptyStateComponent,
  FormDrawerComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillTon,
  optionsDrawer,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';

/**
 * Page « Étudiants » : annuaire des étudiants + gestion complète (CRUD).
 * Liste filtrable avec pastilles de genre/statut, création / modification (drawer
 * de formulaire) et suppression (confirmation), dans le respect de la charte UNCHK.
 * Suit le même schéma que la page Partenaires (page-header + bouton Nouveau,
 * data-table avec actions, form-drawer, confirm-dialog, rechargement).
 */
@Component({
  selector: 'app-etudiants',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './etudiants.component.html',
  styleUrl: './etudiants.component.scss',
})
export class EtudiantsComponent {
  private readonly people = inject(PeopleService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement de la première page d'étudiants (50 lignes).
  protected readonly data = chargerDepuis(() => this.people.listerEtudiants(0, 50));

  // Lignes du tableau : contenu de la page paginée (ou liste vide).
  protected readonly lignes = computed<Etudiant[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Colonnes du tableau, alignées sur les champs du DTO Etudiant.
  // Les colonnes courtes ont une largeur fixe pour éviter les retours à la ligne
  // disgracieux ; nom/prénom/promo restent souples et occupent l'espace restant.
  protected readonly colonnes: ColonneTable<Etudiant>[] = [
    { cle: 'matricule', libelle: 'Matricule', largeur: '120px' },
    { cle: 'lastName', libelle: 'Nom' },
    { cle: 'firstName', libelle: 'Prénom' },
    {
      cle: 'gender',
      libelle: 'Genre',
      type: 'pastille',
      align: 'centre',
      largeur: '110px',
      // On affiche un libellé lisible plutôt que le code brut (M/F).
      valeur: (e) => this.libelleGenre(e.gender),
      ton: (e) => this.tonGenre(e.gender),
    },
    { cle: 'promotion', libelle: 'Promo' },
    { cle: 'enrollmentYear', libelle: 'Année', type: 'nombre', largeur: '90px' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      align: 'centre',
      largeur: '120px',
      ton: (e) => this.tonStatut(e.status),
    },
  ];

  // Options du genre = valeurs exactes de l'enum backend Genre (homme/femme/autre).
  private readonly optionsGenre = [
    { valeur: 'homme', libelle: 'Homme' },
    { valeur: 'femme', libelle: 'Femme' },
    { valeur: 'autre', libelle: 'Autre' },
  ];

  // Options du statut = valeurs exactes de l'enum backend StudentStatus.
  private readonly optionsStatut = [
    { valeur: 'inscrit', libelle: 'Inscrit' },
    { valeur: 'diplome', libelle: 'Diplômé' },
    { valeur: 'suspendu', libelle: 'Suspendu' },
    { valeur: 'abandon', libelle: 'Abandon' },
  ];

  // Champs communs (création + modification) = DTO ModifierEtudiantRequest.
  // Requis selon les @NotBlank/@NotNull du DTO : firstName, lastName, gender, status.
  private readonly champsCommuns: ChampForm[] = [
    { cle: 'firstName', libelle: 'Prénom', requis: true },
    { cle: 'lastName', libelle: 'Nom', requis: true },
    {
      cle: 'gender',
      libelle: 'Genre',
      type: 'select',
      requis: true,
      options: this.optionsGenre,
    },
    { cle: 'matricule', libelle: 'Matricule' },
    { cle: 'promotion', libelle: 'Promotion' },
    { cle: 'enrollmentYear', libelle: 'Année d’inscription', type: 'nombre' },
    { cle: 'exitYear', libelle: 'Année de sortie', type: 'nombre' },
    { cle: 'birthDate', libelle: 'Date de naissance', type: 'date' },
    { cle: 'birthPlace', libelle: 'Lieu de naissance' },
    { cle: 'email', libelle: 'Courriel', type: 'email' },
    { cle: 'phone', libelle: 'Téléphone', type: 'tel' },
    { cle: 'address', libelle: 'Adresse', type: 'textarea' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'select',
      requis: true,
      options: this.optionsStatut,
    },
  ];

  // À la création, l'INE est requis (et immuable : absent à la modification).
  private readonly champsCreation: ChampForm[] = [
    { cle: 'ine', libelle: 'INE', requis: true },
    ...this.champsCommuns,
  ];

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouvel étudiant', this.champsCreation).subscribe((corps) => {
      if (corps) {
        this.ecrire(this.people.creerEtudiant(corps), 'Étudiant créé.');
      }
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli). L'INE n'est pas modifiable. */
  protected onModifier(e: Etudiant): void {
    this.ouvrirForm(
      'Modifier l’étudiant',
      this.champsCommuns,
      e as unknown as Record<string, unknown>
    ).subscribe((corps) => {
      if (corps) {
        this.ecrire(this.people.modifierEtudiant(e.id, corps), 'Étudiant modifié.');
      }
    });
  }

  /** Demande confirmation puis supprime l'étudiant. */
  protected onSupprimer(e: Etudiant): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer l’étudiant',
          message: `Confirmer la suppression de « ${e.firstName} ${e.lastName} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.people.supprimerEtudiant(e.id), 'Étudiant supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    champs: ChampForm[],
    valeurInitiale?: Record<string, unknown>
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(FormDrawerComponent, optionsDrawer({ titre, champs, valeurInitiale }))
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

  // Libellé lisible du genre à partir du code stocké.
  private libelleGenre(genre: string): string {
    if (genre === 'M' || genre === 'homme') {
      return 'Masculin';
    }
    if (genre === 'F' || genre === 'femme') {
      return 'Féminin';
    }
    return genre || '—';
  }

  // Ton de la pastille de genre (purement décoratif, ton info/danger doux).
  private tonGenre(genre: string): StatusPillTon {
    if (genre === 'M' || genre === 'homme') {
      return 'info';
    }
    if (genre === 'F' || genre === 'femme') {
      return 'danger';
    }
    return 'neutre';
  }

  // Ton de la pastille de statut selon l'état de l'étudiant.
  private tonStatut(statut: string): StatusPillTon {
    switch (statut) {
      case 'inscrit':
        return 'succes';
      case 'diplome':
        return 'info';
      case 'suspendu':
        return 'attention';
      case 'abandon':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
