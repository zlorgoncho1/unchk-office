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

import { AdminService, Communique } from '../../core/data';
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
  optionsDrawer,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';

/**
 * Page « Communiqués » : notes de service et circulaires du niveau central.
 * <p>
 * Compteurs en tête puis tableau filtrable avec consultation (détail), édition,
 * suppression et action « Publier ». La publication d'un communiqué déclenche les
 * notifications automatiques aux rôles ciblés (via communication-service). La nature
 * (note de service / circulaire) est figée à la création.
 */
@Component({
  selector: 'app-communiques',
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
  templateUrl: './communiques.component.html',
  styleUrl: './communiques.component.scss',
})
export class CommuniquesComponent {
  private readonly admin = inject(AdminService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement réactif des communiqués.
  protected readonly data = chargerDepuis(() => this.admin.listerCommuniques());

  protected readonly lignes = computed<Communique[]>(
    () => this.data.etat().donnees ?? []
  );

  // Compteurs synthétiques.
  protected readonly nbTotal = computed(() => this.lignes().length);
  protected readonly nbCirculaires = computed(
    () => this.lignes().filter((c) => c.kind === 'circulaire').length
  );
  protected readonly nbNotes = computed(
    () => this.lignes().filter((c) => c.kind === 'note_service').length
  );
  protected readonly nbPublies = computed(
    () => this.lignes().filter((c) => c.published).length
  );

  // Prédicat de visibilité de l'action « Publier » : uniquement les brouillons.
  protected readonly publiable = (c: Communique): boolean => !c.published;

  // Options de la nature (= enum AdminDocKind backend).
  private readonly optionsNature = [
    { valeur: 'note_service', libelle: 'Note de service' },
    { valeur: 'circulaire', libelle: 'Circulaire' },
  ];

  // Préréglages d'audience (rôles destinataires côté serveur).
  private readonly optionsAudience = [
    { valeur: 'tous', libelle: 'Tous' },
    { valeur: 'personnel', libelle: 'Personnel' },
    { valeur: 'enseignants', libelle: 'Enseignants' },
    { valeur: 'etudiants', libelle: 'Étudiants' },
    { valeur: 'administration', libelle: 'Administration' },
  ];

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<Communique>[] = [
    { cle: 'reference', libelle: 'Référence', type: 'texte', largeur: '120px' },
    {
      cle: 'kind',
      libelle: 'Nature',
      type: 'pastille',
      valeur: (c) => (c.kind === 'circulaire' ? 'Circulaire' : 'Note de service'),
      ton: (c) => (c.kind === 'circulaire' ? 'attention' : 'info'),
      largeur: '150px',
    },
    { cle: 'title', libelle: 'Titre' },
    {
      cle: 'audience',
      libelle: 'Audience',
      valeur: (c) => this.libelleAudience(c.audience),
      largeur: '150px',
    },
    { cle: 'issueDate', libelle: 'Émission', type: 'date', largeur: '120px' },
    {
      cle: 'published',
      libelle: 'État',
      type: 'pastille',
      valeur: (c) => (c.published ? 'Publié' : 'Brouillon'),
      ton: (c) => (c.published ? 'succes' : 'neutre'),
      largeur: '120px',
    },
    // Corps affiché dans le drawer de détail uniquement.
    { cle: 'body', libelle: 'Contenu', masquerEnTable: true },
  ];

  /** Ouvre le drawer de création (la nature est figée à la création). */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau communiqué', this.champs(true), {
      kind: 'note_service',
      audience: 'personnel',
    }).subscribe((corps) => {
      if (corps) {
        this.ecrire(this.admin.creerCommunique(corps), 'Communiqué créé (brouillon).');
      }
    });
  }

  /** Ouvre le drawer d'édition pré-rempli (sans la nature, non modifiable). */
  protected onModifier(c: Communique): void {
    const initial: Record<string, unknown> = {
      title: c.title,
      audience: c.audience,
      issueDate: c.issueDate?.slice(0, 10),
      reference: c.reference ?? '',
      body: c.body ?? '',
    };
    this.ouvrirForm('Modifier le communiqué', this.champs(false), initial).subscribe(
      (corps) => {
        if (corps) {
          this.ecrire(this.admin.modifierCommunique(c.id, corps), 'Communiqué mis à jour.');
        }
      }
    );
  }

  /** Publie le communiqué (déclenche les notifications aux rôles ciblés). */
  protected onPublier(c: Communique): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Publier le communiqué',
          message: `Publier « ${c.title} » ? Les destinataires (${this.libelleAudience(
            c.audience
          )}) seront notifiés automatiquement.`,
          libelleConfirmer: 'Publier',
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.admin.publierCommunique(c.id), 'Communiqué publié, destinataires notifiés.');
        }
      });
  }

  /** Demande confirmation puis supprime le communiqué. */
  protected onSupprimer(c: Communique): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le communiqué',
          message: `Confirmer la suppression de « ${c.title} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.admin.supprimerCommunique(c.id), 'Communiqué supprimé.');
        }
      });
  }

  /** Champs du formulaire. La nature n'est présente qu'à la création (figée ensuite). */
  private champs(creation: boolean): ChampForm[] {
    const nature: ChampForm[] = creation
      ? [
          {
            cle: 'kind',
            libelle: 'Nature',
            type: 'select',
            requis: true,
            options: this.optionsNature,
          },
        ]
      : [];
    return [
      ...nature,
      { cle: 'title', libelle: 'Titre', requis: true, largeur: 'pleine' },
      {
        cle: 'audience',
        libelle: 'Audience (destinataires)',
        type: 'select',
        requis: true,
        options: this.optionsAudience,
      },
      { cle: 'issueDate', libelle: "Date d'émission", type: 'date' },
      { cle: 'reference', libelle: 'Référence' },
      { cle: 'body', libelle: 'Contenu', type: 'textarea', largeur: 'pleine' },
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

  /** Libellé lisible d'une audience. */
  private libelleAudience(valeur: string): string {
    return this.optionsAudience.find((o) => o.valeur === valeur)?.libelle ?? 'Personnalisée';
  }
}
