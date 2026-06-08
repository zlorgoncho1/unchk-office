import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { InsertionService, Stage, StatutStage } from '../../core/data';
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
 * Page « Suivi insertion » : liste des stages des étudiants — gestion complète (CRUD).
 * Tableau brandé filtrable + création / modification (drawer de formulaire) /
 * suppression (confirmation). Même schéma que la page Partenaires.
 * Source : InsertionService.listerStages() -> Stage[].
 */
@Component({
  selector: 'app-insertion',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './insertion.component.html',
})
export class InsertionComponent {
  private readonly svc = inject(InsertionService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement des stages (état réactif : chargement / erreur / données).
  protected readonly data = chargerDepuis(() => this.svc.listerStages());

  // Lignes du tableau (liste simple, pas de pagination côté API).
  protected readonly lignes = computed<Stage[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau de suivi des stages.
  // Les colonnes courtes ont une largeur fixe pour laisser respirer
  // les colonnes de texte long (intitulé, maître de stage), qui s'étirent.
  protected readonly colonnes: ColonneTable<Stage>[] = [
    { cle: 'title', libelle: 'Intitulé' },
    { cle: 'supervisorName', libelle: 'Maître de stage' },
    { cle: 'startDate', libelle: 'Début', type: 'date', largeur: '110px' },
    { cle: 'endDate', libelle: 'Fin', type: 'date', largeur: '110px' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      largeur: '130px',
      ton: (s) => this.tonStatut(s.status),
    },
    { cle: 'grade', libelle: 'Note', type: 'nombre', largeur: '90px' },
  ];

  // Champs du formulaire = DTO backend InternshipRequest.
  // Les clés correspondent exactement aux noms du DTO (studentRef, partnerId, etc.).
  private readonly champs: ChampForm[] = [
    { cle: 'title', libelle: 'Intitulé du stage', requis: true, largeur: 'pleine' },
    {
      cle: 'studentRef',
      libelle: 'Étudiant (identifiant)',
      requis: true,
      aide: 'Identifiant de l’étudiant (people.students.id).',
    },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'select',
      options: [
        { valeur: 'prevu', libelle: 'Prévu' },
        { valeur: 'en_cours', libelle: 'En cours' },
        { valeur: 'termine', libelle: 'Terminé' },
        { valeur: 'rompu', libelle: 'Rompu' },
        { valeur: 'valide', libelle: 'Validé' },
      ],
    },
    { cle: 'startDate', libelle: 'Date de début', type: 'date' },
    { cle: 'endDate', libelle: 'Date de fin', type: 'date' },
    { cle: 'supervisorName', libelle: 'Maître de stage' },
    {
      cle: 'partnerId',
      libelle: 'Partenaire (identifiant)',
      aide: 'Identifiant du partenaire d’accueil.',
    },
    {
      cle: 'tutorRef',
      libelle: 'Tuteur académique (identifiant)',
      aide: 'Identifiant du tuteur (people.staff.id).',
    },
    { cle: 'grade', libelle: 'Note (0 à 20)', type: 'nombre' },
  ];

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau stage').subscribe((corps) => {
      if (corps) {
        // status:prevu par défaut si non renseigné (défaut métier côté backend).
        this.ecrire(
          this.svc.creerStage({ status: 'prevu', ...corps }),
          'Stage créé.'
        );
      }
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli). */
  protected onModifier(s: Stage): void {
    this.ouvrirForm('Modifier le stage', s).subscribe((corps) => {
      if (corps) {
        this.ecrire(this.svc.modifierStage(s.id, corps), 'Stage modifié.');
      }
    });
  }

  /** Demande confirmation puis supprime le stage. */
  protected onSupprimer(s: Stage): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le stage',
          message: `Confirmer la suppression de « ${s.title} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimerStage(s.id), 'Stage supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    s?: Stage
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({
          titre,
          champs: this.champs,
          valeurInitiale: s as unknown as Record<string, unknown>,
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

  // Ton de la pastille selon le statut du stage.
  private tonStatut(statut: StatutStage): StatusPillTon {
    switch (statut) {
      case 'prevu':
        return 'neutre';
      case 'en_cours':
        return 'attention';
      case 'termine':
        return 'info';
      case 'valide':
        return 'succes';
      case 'rompu':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
