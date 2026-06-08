import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import {
  AcademicService,
  Creneau,
  EmploiTempsService,
  Formation,
} from '../../core/data';
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
import { humaniser } from '../../shared/util/format.util';
import { chargerDepuis } from '../../shared/util/loadable';

/**
 * Page « Emplois du temps » — consultation et gestion des créneaux par formation.
 * <p>
 * On choisit d'abord une formation (mat-select alimenté par AcademicService), puis on
 * affiche ses créneaux dans un data-table. Création (drawer) et suppression (confirmation)
 * sont câblées sur l'API des créneaux. Même schéma que la page « Partenaires ».
 */
@Component({
  selector: 'app-emplois-du-temps',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
    DataTableComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './emplois-du-temps.component.html',
  styleUrl: './emplois-du-temps.component.scss',
})
export class EmploisDuTempsComponent {
  private readonly academic = inject(AcademicService);
  private readonly svc = inject(EmploiTempsService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement de la liste des formations (pour le sélecteur).
  protected readonly formations = chargerDepuis(() =>
    this.academic.listerFormations()
  );

  protected readonly listeFormations = computed<Formation[]>(
    () => this.formations.etat().donnees ?? []
  );

  // Formation sélectionnée (id) ; pilote le chargement des créneaux.
  protected readonly formationId = signal<string | null>(null);

  constructor() {
    // Présélectionne la première formation dès que la liste est disponible,
    // puis charge ses créneaux.
    effect(() => {
      const liste = this.listeFormations();
      if (!this.formationId() && liste.length > 0) {
        this.formationId.set(liste[0].id);
        this.creneaux.recharger();
      }
    });
  }

  // Créneaux de la formation choisie ; rechargé à chaque changement de formation.
  protected readonly creneaux = chargerDepuis(() => this.chargerCreneaux());

  protected readonly lignes = computed<Creneau[]>(
    () => this.creneaux.etat().donnees ?? []
  );

  // Colonnes du tableau des créneaux.
  protected readonly colonnes: ColonneTable<Creneau>[] = [
    { cle: 'courseLabel', libelle: 'Cours' },
    {
      cle: 'dayOfWeek',
      libelle: 'Jour',
      valeur: (c) => (c.dayOfWeek ? humaniser(c.dayOfWeek) : null),
      largeur: '120px',
    },
    {
      cle: 'sessionDate',
      libelle: 'Date',
      type: 'date',
      valeur: (c) => c.sessionDate,
      largeur: '120px',
    },
    {
      cle: 'startTime',
      libelle: 'Début',
      valeur: (c) => this.heure(c.startTime),
      largeur: '90px',
    },
    {
      cle: 'endTime',
      libelle: 'Fin',
      valeur: (c) => this.heure(c.endTime),
      largeur: '90px',
    },
    { cle: 'room', libelle: 'Salle', valeur: (c) => c.room, largeur: '140px' },
    {
      cle: 'formateurNom',
      libelle: 'Intervenant',
      valeur: (c) => c.formateurNom,
    },
  ];

  // Champs du formulaire = CreneauCreationDto backend.
  private readonly champs: ChampForm[] = [
    {
      cle: 'courseLabel',
      libelle: 'Intitulé du cours',
      requis: true,
      largeur: 'pleine',
    },
    {
      cle: 'dayOfWeek',
      libelle: 'Jour (créneau récurrent)',
      type: 'select',
      options: [
        { valeur: 'lundi', libelle: 'Lundi' },
        { valeur: 'mardi', libelle: 'Mardi' },
        { valeur: 'mercredi', libelle: 'Mercredi' },
        { valeur: 'jeudi', libelle: 'Jeudi' },
        { valeur: 'vendredi', libelle: 'Vendredi' },
        { valeur: 'samedi', libelle: 'Samedi' },
        { valeur: 'dimanche', libelle: 'Dimanche' },
      ],
      aide: 'À renseigner pour un créneau récurrent (sinon, indiquez une date).',
    },
    {
      cle: 'sessionDate',
      libelle: 'Date (séance ponctuelle)',
      type: 'date',
      aide: 'Exclusif avec le jour récurrent.',
    },
    { cle: 'startTime', libelle: 'Heure de début', type: 'tel', requis: true, placeholder: 'HH:MM' },
    { cle: 'endTime', libelle: 'Heure de fin', type: 'tel', requis: true, placeholder: 'HH:MM' },
    { cle: 'room', libelle: 'Salle ou lien visio' },
    { cle: 'formateurRef', libelle: 'Intervenant (identifiant)' },
  ];

  /** Change la formation affichée et recharge ses créneaux. */
  protected onChangerFormation(id: string): void {
    this.formationId.set(id);
    this.creneaux.recharger();
  }

  /** Ouvre le drawer de création d'un créneau pour la formation courante. */
  protected nouveau(): void {
    const id = this.formationId();
    if (!id) {
      return;
    }
    this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre: 'Nouveau créneau', champs: this.champs })
      )
      .afterClosed()
      .subscribe((corps) => {
        if (corps) {
          this.ecrire(
            this.svc.ajouterCreneau(id, corps as Record<string, unknown>),
            'Créneau ajouté.'
          );
        }
      });
  }

  /** Demande confirmation puis supprime le créneau. */
  protected onSupprimer(c: Creneau): void {
    const id = this.formationId();
    if (!id) {
      return;
    }
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le créneau',
          message: `Confirmer la suppression de « ${c.courseLabel} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimerCreneau(id, c.id), 'Créneau supprimé.');
        }
      });
  }

  /** Charge les créneaux de la formation courante (vide si aucune sélection). */
  private chargerCreneaux(): Observable<Creneau[]> {
    const id = this.formationId();
    if (!id) {
      // Pas de formation choisie : renvoie une liste vide sans appel HTTP.
      return new Observable<Creneau[]>((s) => {
        s.next([]);
        s.complete();
      });
    }
    return this.svc.listerCreneaux(id);
  }

  /** Exécute une écriture, notifie et recharge les créneaux. */
  private ecrire(source$: Observable<unknown>, messageOk: string): void {
    source$.subscribe({
      next: () => {
        this.snack.open(messageOk, 'OK', { duration: 3000 });
        this.creneaux.recharger();
      },
      error: () =>
        this.snack.open(
          'Action impossible (droits insuffisants ou données invalides).',
          'OK',
          { duration: 4000 }
        ),
    });
  }

  /** Tronque une heure « HH:mm:ss » en « HH:mm » pour l'affichage. */
  private heure(valeur: string | null): string | null {
    return valeur ? valeur.slice(0, 5) : null;
  }
}
