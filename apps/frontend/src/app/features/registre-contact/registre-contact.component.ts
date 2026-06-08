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
import { Observable, of } from 'rxjs';

import {
  ContactRegistre,
  Etudiant,
  InsertionService,
  PeopleService,
  SituationInsertion,
  SituationInsertionDto,
} from '../../core/data';
import {
  ChampForm,
  ColonneTable,
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
 * Page « Registre de contact » — suivi du devenir des diplômés.
 * <p>
 * On choisit d'abord un étudiant (mat-select alimenté par PeopleService), puis on affiche :
 *  - son registre de contact (échanges de suivi, en lecture/création),
 *  - ses situations d'insertion (saisie/édition, qui alimentent les statistiques
 *    auto-emploi vs emploi salarié).
 * Même schéma que la page « Emplois du temps » (sélecteur) et « Partenaires » (CRUD par drawer).
 */
@Component({
  selector: 'app-registre-contact',
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
  templateUrl: './registre-contact.component.html',
  styleUrl: './registre-contact.component.scss',
})
export class RegistreContactComponent {
  private readonly people = inject(PeopleService);
  private readonly svc = inject(InsertionService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement de la liste des étudiants (pour le sélecteur).
  protected readonly etudiants = chargerDepuis(() =>
    this.people.listerEtudiants(0, 100)
  );

  protected readonly listeEtudiants = computed<Etudiant[]>(
    () => this.etudiants.etat().donnees?.content ?? []
  );

  // Étudiant sélectionné (id) ; pilote le chargement des contacts et des situations.
  protected readonly etudiantId = signal<string | null>(null);

  constructor() {
    // Présélectionne le premier étudiant dès que la liste est disponible,
    // puis charge son registre et ses situations.
    effect(() => {
      const liste = this.listeEtudiants();
      if (!this.etudiantId() && liste.length > 0) {
        this.etudiantId.set(liste[0].id);
        this.contacts.recharger();
        this.situations.recharger();
      }
    });
  }

  // Registre de contact de l'étudiant courant.
  protected readonly contacts = chargerDepuis(() => this.chargerContacts());

  protected readonly lignesContacts = computed<ContactRegistre[]>(
    () => this.contacts.etat().donnees ?? []
  );

  // Situations d'insertion de l'étudiant courant.
  protected readonly situations = chargerDepuis(() => this.chargerSituations());

  protected readonly lignesSituations = computed<SituationInsertionDto[]>(
    () => this.situations.etat().donnees ?? []
  );

  // Colonnes du registre de contact.
  protected readonly colonnesContacts: ColonneTable<ContactRegistre>[] = [
    { cle: 'contactedAt', libelle: 'Date', type: 'date', largeur: '120px' },
    { cle: 'channel', libelle: 'Canal', largeur: '160px' },
    { cle: 'notes', libelle: 'Compte rendu de l’échange' },
  ];

  // Colonnes des situations d'insertion.
  protected readonly colonnesSituations: ColonneTable<SituationInsertionDto>[] = [
    {
      cle: 'kind',
      libelle: 'Situation',
      type: 'pastille',
      largeur: '170px',
      valeur: (s) => this.libelleSituation(s.kind),
      ton: (s) => this.tonSituation(s.kind),
    },
    { cle: 'employerName', libelle: 'Employeur / structure' },
    { cle: 'jobTitle', libelle: 'Intitulé du poste' },
    { cle: 'observedAt', libelle: 'Constatée le', type: 'date', largeur: '130px' },
    {
      cle: 'current',
      libelle: 'Courante',
      type: 'pastille',
      align: 'centre',
      largeur: '110px',
      valeur: (s) => (s.current ? 'Oui' : 'Non'),
      ton: (s) => (s.current ? 'succes' : 'neutre'),
    },
  ];

  // Options de situation d'insertion (réutilisées dans le formulaire et l'affichage).
  private readonly OPTIONS_SITUATION: { valeur: SituationInsertion; libelle: string }[] = [
    { valeur: 'emploi_salarie', libelle: 'Emploi salarié' },
    { valeur: 'auto_emploi', libelle: 'Auto-emploi' },
    { valeur: 'recherche_emploi', libelle: 'En recherche d’emploi' },
    { valeur: 'poursuite_etudes', libelle: 'Poursuite d’études' },
    { valeur: 'sans_activite', libelle: 'Sans activité' },
  ];

  /** Change l'étudiant affiché et recharge son registre et ses situations. */
  protected onChangerEtudiant(id: string): void {
    this.etudiantId.set(id);
    this.contacts.recharger();
    this.situations.recharger();
  }

  // --- Registre de contact ---

  /** Ouvre le drawer d'enregistrement d'un contact pour l'étudiant courant. */
  protected nouveauContact(): void {
    const id = this.etudiantId();
    if (!id) {
      return;
    }
    const champs: ChampForm[] = [
      {
        cle: 'channel',
        libelle: 'Canal',
        type: 'select',
        options: [
          { valeur: 'téléphone', libelle: 'Téléphone' },
          { valeur: 'email', libelle: 'Courriel' },
          { valeur: 'présentiel', libelle: 'Présentiel' },
          { valeur: 'réseaux sociaux', libelle: 'Réseaux sociaux' },
        ],
      },
      { cle: 'contactedAt', libelle: 'Date du contact', type: 'date' },
      { cle: 'notes', libelle: 'Compte rendu de l’échange', type: 'textarea', largeur: 'pleine' },
    ];
    this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre: 'Nouveau contact', champs })
      )
      .afterClosed()
      .subscribe((corps) => {
        if (corps) {
          // studentRef ajouté côté front : le formulaire ne porte que l'échange.
          this.ecrireContact(
            this.svc.enregistrerContact({
              studentRef: id,
              ...(corps as Record<string, unknown>),
            }),
            'Contact enregistré.'
          );
        }
      });
  }

  // --- Situations d'insertion ---

  /** Ouvre le drawer de déclaration d'une situation pour l'étudiant courant. */
  protected nouvelleSituation(): void {
    const id = this.etudiantId();
    if (!id) {
      return;
    }
    this.ouvrirFormSituation('Déclarer une situation').subscribe((corps) => {
      if (corps) {
        this.ecrireSituation(
          this.svc.declarerSituation({ studentRef: id, current: true, ...corps }),
          'Situation déclarée.'
        );
      }
    });
  }

  /** Ouvre le drawer d'édition d'une situation existante (pré-rempli). */
  protected onModifierSituation(s: SituationInsertionDto): void {
    this.ouvrirFormSituation('Modifier la situation', s).subscribe((corps) => {
      if (corps) {
        // On conserve l'étudiant rattaché à la situation.
        this.ecrireSituation(
          this.svc.modifierSituation(s.id, { studentRef: s.studentRef, ...corps }),
          'Situation modifiée.'
        );
      }
    });
  }

  /** Drawer de saisie d'une situation (champs = InsertionOutcomeRequest hors studentRef). */
  private ouvrirFormSituation(
    titre: string,
    s?: SituationInsertionDto
  ): Observable<Record<string, unknown> | undefined> {
    const champs: ChampForm[] = [
      {
        cle: 'kind',
        libelle: 'Situation d’insertion',
        type: 'select',
        requis: true,
        options: this.OPTIONS_SITUATION,
        largeur: 'pleine',
      },
      { cle: 'employerName', libelle: 'Employeur / structure' },
      { cle: 'jobTitle', libelle: 'Intitulé du poste' },
      { cle: 'observedAt', libelle: 'Date de constat', type: 'date' },
      {
        cle: 'formationRef',
        libelle: 'Formation (identifiant)',
        aide: 'Renseigner pour les statistiques par formation (academic.formations.id).',
      },
    ];
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({
          titre,
          champs,
          valeurInitiale: s as unknown as Record<string, unknown>,
        })
      )
      .afterClosed();
  }

  // --- Chargement piloté par l'étudiant sélectionné ---

  /** Charge le registre de contact de l'étudiant courant (vide si aucune sélection). */
  private chargerContacts(): Observable<ContactRegistre[]> {
    const id = this.etudiantId();
    return id ? this.svc.historiqueContacts(id) : of<ContactRegistre[]>([]);
  }

  /** Charge les situations de l'étudiant courant (vide si aucune sélection). */
  private chargerSituations(): Observable<SituationInsertionDto[]> {
    const id = this.etudiantId();
    return id ? this.svc.situationsEtudiant(id) : of<SituationInsertionDto[]>([]);
  }

  /** Exécute une écriture sur le registre, notifie et recharge les contacts. */
  private ecrireContact(source$: Observable<unknown>, messageOk: string): void {
    source$.subscribe({
      next: () => {
        this.snack.open(messageOk, 'OK', { duration: 3000 });
        this.contacts.recharger();
      },
      error: () => this.erreurAction(),
    });
  }

  /** Exécute une écriture sur une situation, notifie et recharge les situations. */
  private ecrireSituation(source$: Observable<unknown>, messageOk: string): void {
    source$.subscribe({
      next: () => {
        this.snack.open(messageOk, 'OK', { duration: 3000 });
        this.situations.recharger();
      },
      error: () => this.erreurAction(),
    });
  }

  /** Notification d'échec commune (droits insuffisants ou données invalides). */
  private erreurAction(): void {
    this.snack.open(
      'Action impossible (droits insuffisants ou données invalides).',
      'OK',
      { duration: 4000 }
    );
  }

  /** Libellé lisible d'une situation d'insertion. */
  private libelleSituation(kind: SituationInsertion): string {
    return this.OPTIONS_SITUATION.find((o) => o.valeur === kind)?.libelle ?? kind;
  }

  /** Ton de la pastille selon la situation d'insertion. */
  private tonSituation(kind: SituationInsertion): StatusPillTon {
    switch (kind) {
      case 'emploi_salarie':
        return 'succes';
      case 'auto_emploi':
        return 'info';
      case 'poursuite_etudes':
        return 'attention';
      case 'recherche_emploi':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
