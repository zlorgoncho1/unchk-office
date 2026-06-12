import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AcademicService, DiplomeDto, Etudiant, Formation, PeopleService } from '../../core/data';
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
import { DiplomesDialog } from './diplomes-dialog.component';

/**
 * Page « Étudiants » : annuaire des étudiants + gestion complète (CRUD).
 * Liste filtrable avec pastilles de genre/statut, création / modification (drawer
 * de formulaire), gestion des diplômes (dialog dédié) et suppression (confirmation),
 * dans le respect de la charte UNCHK.
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
export class EtudiantsComponent implements OnInit {
  private readonly people = inject(PeopleService);
  private readonly academic = inject(AcademicService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement de la première page d'étudiants (50 lignes).
  protected readonly data = chargerDepuis(() => this.people.listerEtudiants(0, 50));

  // Lignes du tableau : contenu de la page paginée (ou liste vide).
  protected readonly lignes = computed<Etudiant[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Catalogue des formations (chargé au démarrage) : sert au libellé affiché
  // et aux options du champ select « formationRef » du formulaire étudiant.
  private formations: Formation[] = [];

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
    // Formation principale : libellé résolu depuis le catalogue academic.
    { cle: 'formationRef', libelle: 'Formation', valeur: (e) => this.libelleFormation(e.formationRef) },
    { cle: 'promotion', libelle: 'Promo' },
    // « texte » : une année s'affiche sans séparateur de milliers (« 2024 »).
    { cle: 'enrollmentYear', libelle: 'Année', type: 'texte', largeur: '90px' },
    // Nombre de diplômes enregistrés (édités via le dialog dédié).
    {
      cle: 'diplomas',
      libelle: 'Diplômes',
      type: 'nombre',
      align: 'centre',
      largeur: '100px',
      valeur: (e) => e.diplomas?.length ?? 0,
    },
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

  // Options du champ « formationRef » : alimentées dynamiquement depuis le catalogue
  // academic (valeur = id formation, libelle = intitulé). Vides tant que le chargement
  // n'est pas terminé, complétées dans ngOnInit.
  private optionsFormation: { valeur: string; libelle: string }[] = [];

  /** Charge le catalogue des formations au démarrage (pour le select et l'affichage). */
  ngOnInit(): void {
    this.academic.listerFormations().subscribe({
      next: (formations) => {
        this.formations = formations;
        this.optionsFormation = formations.map((f) => ({
          valeur: f.id,
          libelle: f.label,
        }));
      },
      // En cas d'échec, on laisse les options vides : le formulaire reste utilisable
      // sans le choix de la formation (champ facultatif côté backend).
      error: () => undefined,
    });
  }

  // Champs communs (création + modification) = DTO ModifierEtudiantRequest.
  // Requis selon les @NotBlank/@NotNull du DTO : firstName, lastName, gender, status.
  // « formationRef » et « otherTrainings » couvrent le module Étudiant de l'énoncé.
  private champsCommuns(): ChampForm[] {
    return [
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
      // Formation principale : select alimenté par le catalogue academic.
      {
        cle: 'formationRef',
        libelle: 'Formation',
        type: 'select',
        options: this.optionsFormation,
      },
      { cle: 'promotion', libelle: 'Promotion' },
      { cle: 'enrollmentYear', libelle: 'Année d’inscription', type: 'nombre' },
      { cle: 'exitYear', libelle: 'Année de sortie', type: 'nombre' },
      { cle: 'birthDate', libelle: 'Date de naissance', type: 'date' },
      { cle: 'birthPlace', libelle: 'Lieu de naissance' },
      { cle: 'email', libelle: 'Courriel', type: 'email' },
      { cle: 'phone', libelle: 'Téléphone', type: 'tel' },
      { cle: 'address', libelle: 'Adresse', type: 'textarea' },
      // Autres formations suivies (texte libre, hors catalogue).
      { cle: 'otherTrainings', libelle: 'Autres formations', type: 'textarea' },
      {
        cle: 'status',
        libelle: 'Statut',
        type: 'select',
        requis: true,
        options: this.optionsStatut,
      },
    ];
  }

  // À la création, l'INE est requis (et immuable : absent à la modification).
  private champsCreation(): ChampForm[] {
    return [{ cle: 'ine', libelle: 'INE', requis: true }, ...this.champsCommuns()];
  }

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouvel étudiant', this.champsCreation()).subscribe((corps) => {
      if (corps) {
        this.ecrire(this.people.creerEtudiant(corps), 'Étudiant créé.');
      }
    });
  }

  /**
   * Action « Modifier » d'une ligne : ouvre le drawer d'édition des informations
   * (pré-rempli, INE non modifiable), puis enchaîne sur le dialog des diplômes.
   * Ce point d'entrée unique (le tableau générique n'expose qu'une action par ligne)
   * permet de gérer à la fois les champs scalaires et la liste imbriquée des diplômes.
   */
  protected onModifier(e: Etudiant): void {
    this.ouvrirForm(
      'Modifier l’étudiant',
      this.champsCommuns(),
      e as unknown as Record<string, unknown>
    ).subscribe((corps) => {
      if (!corps) {
        return;
      }
      // On préserve les diplômes existants (le formulaire scalaire ne les édite pas).
      const payload = { ...corps, diplomas: this.diplomesBruts(e) };
      this.people.modifierEtudiant(e.id, payload).subscribe({
        next: () => {
          this.snack.open('Étudiant modifié.', 'OK', { duration: 3000 });
          this.data.recharger();
          // Étudiant à jour = ligne d'origine + champs scalaires fraîchement saisis :
          // garantit que la gestion des diplômes ne revertira pas ces modifications.
          const aJour = { ...e, ...corps } as unknown as Etudiant;
          this.gererDiplomes(aJour);
        },
        error: () => this.erreurEcriture(),
      });
    });
  }

  /**
   * Ouvre le dialog dédié d'édition des diplômes (liste imbriquée non gérable par
   * le form-drawer générique). À la validation, renvoie le tableau de diplômes et
   * met à jour l'étudiant via {@code modifierEtudiant} en conservant ses autres champs.
   */
  private gererDiplomes(e: Etudiant): void {
    this.dialog
      .open(
        DiplomesDialog,
        optionsDrawer({
          nomComplet: `${e.firstName} ${e.lastName}`,
          diplomes: e.diplomas ?? [],
        })
      )
      .afterClosed()
      .subscribe((diplomas: DiplomeDto[] | undefined) => {
        if (diplomas) {
          // On renvoie l'ensemble des champs requis du DTO + la nouvelle liste de diplômes.
          this.ecrire(
            this.people.modifierEtudiant(e.id, { ...this.champsScalaires(e), diplomas }),
            'Diplômes mis à jour.'
          );
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
      error: () => this.erreurEcriture(),
    });
  }

  /** Message d'erreur générique pour une écriture refusée ou invalide. */
  private erreurEcriture(): void {
    this.snack.open(
      'Action impossible (droits insuffisants ou données invalides).',
      'OK',
      { duration: 4000 }
    );
  }

  /**
   * Champs scalaires requis du DTO ModifierEtudiantRequest pour un étudiant donné.
   * Utilisé quand on met à jour uniquement les diplômes : on doit renvoyer les
   * champs obligatoires (firstName, lastName, gender, status) inchangés.
   */
  private champsScalaires(e: Etudiant): Record<string, unknown> {
    return {
      matricule: e.matricule,
      firstName: e.firstName,
      lastName: e.lastName,
      gender: e.gender,
      birthDate: e.birthDate,
      birthPlace: e.birthPlace,
      email: e.email,
      phone: e.phone,
      address: e.address,
      formationRef: e.formationRef,
      promotion: e.promotion,
      enrollmentYear: e.enrollmentYear,
      exitYear: e.exitYear,
      otherTrainings: e.otherTrainings,
      status: e.status,
    };
  }

  /** Diplômes existants au format attendu par le backend (sans champs systèmes). */
  private diplomesBruts(e: Etudiant): unknown[] {
    return (e.diplomas ?? []).map((d) => ({
      label: d.label,
      level: d.level || null,
      obtainedAt: d.obtainedAt,
    }));
  }

  // Libellé lisible d'une formation à partir de sa référence (UUID).
  private libelleFormation(ref: string | null): string {
    if (!ref) {
      return '—';
    }
    const f = this.formations.find((x) => x.id === ref);
    return f ? f.label : '—';
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
