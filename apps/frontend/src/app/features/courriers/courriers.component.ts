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

import { AdminService, Courrier, StatutCourrier } from '../../core/data';
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
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Courrier » : registre du courrier arrivé / départ (module Administration).
 * <p>
 * Compteurs en tête (total, arrivés, départs, en cours) puis tableau filtrable avec
 * consultation (drawer de détail), édition, changement de statut et suppression. Même
 * gabarit que la page Budgets (page-header + stat-cards + data-table + form-drawer).
 * <p>
 * Le sens du courrier est figé à la création. Le statut suit un endpoint dédié : le
 * composant déclenche le PATCH de statut après l'écriture quand le statut choisi diffère.
 */
@Component({
  selector: 'app-courriers',
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
  templateUrl: './courriers.component.html',
  styleUrl: './courriers.component.scss',
})
export class CourriersComponent {
  private readonly admin = inject(AdminService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement réactif des courriers (chargement / succès / erreur).
  protected readonly data = chargerDepuis(() => this.admin.listerCourriers());

  // Lignes du tableau.
  protected readonly lignes = computed<Courrier[]>(
    () => this.data.etat().donnees ?? []
  );

  // Compteurs synthétiques.
  protected readonly nbCourriers = computed(() => this.lignes().length);
  protected readonly nbArrives = computed(
    () => this.lignes().filter((c) => c.direction === 'arrive').length
  );
  protected readonly nbDeparts = computed(
    () => this.lignes().filter((c) => c.direction === 'depart').length
  );
  protected readonly nbEnCours = computed(
    () =>
      this.lignes().filter(
        (c) => c.status === 'recu' || c.status === 'en_traitement'
      ).length
  );

  // Options du sens (= enum MailDirection backend).
  private readonly optionsDirection = [
    { valeur: 'arrive', libelle: 'Arrivé' },
    { valeur: 'depart', libelle: 'Départ' },
  ];

  // Options du statut (= enum MailStatus backend).
  private readonly optionsStatut = [
    { valeur: 'recu', libelle: 'Reçu' },
    { valeur: 'en_traitement', libelle: 'En traitement' },
    { valeur: 'traite', libelle: 'Traité' },
    { valeur: 'archive', libelle: 'Archivé' },
    { valeur: 'clos', libelle: 'Clos' },
  ];

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<Courrier>[] = [
    { cle: 'reference', libelle: 'Référence', type: 'texte', largeur: '120px' },
    {
      cle: 'direction',
      libelle: 'Sens',
      type: 'pastille',
      valeur: (c) => humaniser(c.direction),
      ton: (c) => (c.direction === 'arrive' ? 'info' : 'neutre'),
      largeur: '110px',
    },
    { cle: 'subject', libelle: 'Objet' },
    { cle: 'correspondent', libelle: 'Correspondant', largeur: '220px' },
    { cle: 'mailDate', libelle: 'Date', type: 'date', largeur: '120px' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (c) => humaniser(c.status),
      ton: (c) => this.tonStatut(c.status),
      largeur: '140px',
    },
    // Annotations affichées dans le drawer de détail uniquement.
    { cle: 'notes', libelle: 'Annotations', masquerEnTable: true },
  ];

  /** Ouvre le drawer de création (le sens est figé à la création). */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau courrier', this.champs(true), { status: 'recu' }).subscribe(
      (corps) => {
        if (corps) {
          // CreationMailDto accepte le statut : on POST le corps tel quel.
          this.ecrire(this.admin.creerCourrier(corps), 'Courrier enregistré.');
        }
      }
    );
  }

  /** Ouvre le drawer d'édition pré-rempli (sans le sens, non modifiable). */
  protected onModifier(c: Courrier): void {
    const initial: Record<string, unknown> = {
      subject: c.subject,
      correspondent: c.correspondent,
      mailDate: c.mailDate?.slice(0, 10),
      status: c.status,
      reference: c.reference ?? '',
      notes: c.notes ?? '',
    };
    this.ouvrirForm('Modifier le courrier', this.champs(false), initial).subscribe(
      (corps) => {
        if (!corps) {
          return;
        }
        // MajMailDto ne porte pas le statut : on l'extrait et on l'applique séparément.
        const { status, ...entete } = corps;
        const cible = (status as StatutCourrier) ?? c.status;
        const maj$ = this.admin.modifierCourrier(c.id, entete).pipe(
          switchMap((m) =>
            cible !== c.status ? this.admin.changerStatutCourrier(m.id, cible) : of(m)
          )
        );
        this.ecrire(maj$, 'Courrier mis à jour.');
      }
    );
  }

  /** Demande confirmation puis supprime le courrier. */
  protected onSupprimer(c: Courrier): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le courrier',
          message: `Confirmer la suppression du courrier « ${c.subject} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.admin.supprimerCourrier(c.id), 'Courrier supprimé.');
        }
      });
  }

  /** Champs du formulaire. Le sens n'est présent qu'à la création (figé ensuite). */
  private champs(creation: boolean): ChampForm[] {
    const sens: ChampForm[] = creation
      ? [
          {
            cle: 'direction',
            libelle: 'Sens',
            type: 'select',
            requis: true,
            options: this.optionsDirection,
          },
        ]
      : [];
    return [
      ...sens,
      { cle: 'subject', libelle: 'Objet du courrier', requis: true, largeur: 'pleine' },
      { cle: 'correspondent', libelle: 'Correspondant', requis: true },
      { cle: 'mailDate', libelle: 'Date du courrier', type: 'date', requis: true },
      {
        cle: 'status',
        libelle: 'Statut',
        type: 'select',
        requis: true,
        options: this.optionsStatut,
      },
      { cle: 'reference', libelle: 'Référence (n° de courrier)' },
      { cle: 'notes', libelle: 'Annotations', type: 'textarea', largeur: 'pleine' },
    ];
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

  // Ton de la pastille selon le statut du courrier.
  private tonStatut(statut: StatutCourrier): StatusPillTon {
    switch (statut) {
      case 'en_traitement':
        return 'attention';
      case 'traite':
        return 'info';
      case 'clos':
        return 'succes';
      case 'archive':
        return 'neutre';
      default:
        return 'neutre';
    }
  }
}
