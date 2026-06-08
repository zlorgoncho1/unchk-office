import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AuthService } from '../../core/auth';
import { CommunicationService, Reunion } from '../../core/data';
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
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Réunions » : liste tabulaire des réunions de communication + création.
 * Source : CommunicationService.listerReunions() (tableau Reunion[]).
 * Tableau brandé filtrable avec pastilles de type et de statut.
 *
 * Gestion complète : création, modification (drawer pré-rempli) et suppression (confirmation).
 */
@Component({
  selector: 'app-reunions',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './reunions.component.html',
  styleUrl: '../home/home-shared.scss',
})
export class ReunionsComponent {
  private readonly svc = inject(CommunicationService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Ressource réactive : la liste des réunions via le gateway.
  protected readonly data = chargerDepuis(() => this.svc.listerReunions());

  // Lignes du tableau (la source renvoie un simple tableau).
  protected readonly lignes = computed<Reunion[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau de réunions.
  protected readonly colonnes: ColonneTable<Reunion>[] = [
    // Colonne de texte long : laissée libre de s'étirer.
    { cle: 'title', libelle: 'Titre' },
    {
      cle: 'type',
      libelle: 'Type',
      type: 'pastille',
      valeur: (r) => humaniser(r.type),
      ton: () => 'info',
      largeur: '140px', // pastille courte : largeur fixe
    },
    { cle: 'startsAt', libelle: 'Début', type: 'date-heure', largeur: '150px' }, // date+heure : largeur fixe
    // Colonne de texte : laissée libre de s'étirer.
    { cle: 'location', libelle: 'Lieu' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (r) => humaniser(r.status),
      ton: (r) => this.tonStatut(r),
      largeur: '120px', // pastille courte : largeur fixe
    },
    // Colonne de texte : laissée libre de s'étirer.
    { cle: 'organizerName', libelle: 'Organisateur' },
  ];

  // Champs du formulaire = sous-ensemble simple de ReunionCreationRequest.
  // On omet les participants (optionnels) : champs simples uniquement.
  private readonly champs: ChampForm[] = [
    { cle: 'title', libelle: 'Titre', requis: true },
    {
      cle: 'type',
      libelle: 'Type',
      type: 'select',
      requis: true,
      options: [
        { valeur: 'reunion', libelle: 'Réunion' },
        { valeur: 'seminaire', libelle: 'Séminaire' },
        { valeur: 'webinaire', libelle: 'Webinaire' },
        { valeur: 'conseil_universite', libelle: "Conseil d'Université" },
        { valeur: 'tutorat', libelle: 'Tutorat' },
        { valeur: 'preparation_cours', libelle: 'Préparation de cours' },
        { valeur: 'evaluation', libelle: 'Évaluation' },
      ],
    },
    { cle: 'startsAt', libelle: 'Début', type: 'date', requis: true },
    { cle: 'endsAt', libelle: 'Fin', type: 'date' },
    { cle: 'location', libelle: 'Lieu (salle ou lien visio)' },
  ];

  /** Ouvre le drawer de création d'une réunion. */
  protected nouveau(): void {
    this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre: 'Nouvelle réunion', champs: this.champs })
      )
      .afterClosed()
      .subscribe((corps?: Record<string, unknown>) => {
        if (corps) {
          // organizerId est requis par le DTO : on prend l'UUID de l'utilisateur courant.
          // startsAt/endsAt sont des OffsetDateTime : on convertit la date saisie en ISO.
          const charge = {
            ...corps,
            startsAt: this.versDateHeure(corps['startsAt']),
            endsAt: this.versDateHeure(corps['endsAt']),
            organizerId: this.auth.currentUser()?.id,
          };
          this.ecrire(this.svc.creerReunion(charge), 'Réunion planifiée.');
        }
      });
  }

  /** Ouvre le drawer d'édition d'une réunion (pré-rempli ; dates ramenées en YYYY-MM-DD). */
  protected onModifier(r: Reunion): void {
    const initial = {
      title: r.title,
      type: r.type,
      startsAt: r.startsAt ? r.startsAt.slice(0, 10) : '',
      endsAt: r.endsAt ? r.endsAt.slice(0, 10) : '',
      location: r.location ?? '',
    };
    this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre: 'Modifier la réunion', champs: this.champs, valeurInitiale: initial })
      )
      .afterClosed()
      .subscribe((corps?: Record<string, unknown>) => {
        if (corps) {
          const charge = {
            ...corps,
            startsAt: this.versDateHeure(corps['startsAt']),
            endsAt: this.versDateHeure(corps['endsAt']),
            organizerId: r.organizerId ?? this.auth.currentUser()?.id,
          };
          this.ecrire(this.svc.modifierReunion(r.id, charge), 'Réunion modifiée.');
        }
      });
  }

  /** Demande confirmation puis supprime la réunion. */
  protected onSupprimer(r: Reunion): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer la réunion',
          message: `Confirmer la suppression de « ${r.title} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimerReunion(r.id), 'Réunion supprimée.');
        }
      });
  }

  /** Convertit une date « YYYY-MM-DD » en OffsetDateTime ISO (minuit UTC), ou undefined. */
  private versDateHeure(valeur: unknown): string | undefined {
    if (typeof valeur === 'string' && valeur !== '') {
      return `${valeur}T00:00:00Z`;
    }
    return undefined;
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

  // Ton de la pastille selon le statut de la réunion.
  private tonStatut(r: Reunion): StatusPillTon {
    switch (r.status) {
      case 'planifiee':
        return 'info';
      case 'en_cours':
        return 'attention';
      case 'terminee':
        return 'succes';
      case 'annulee':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
