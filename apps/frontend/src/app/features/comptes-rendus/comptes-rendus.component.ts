import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AuthService } from '../../core/auth';
import { CommunicationService, CompteRendu } from '../../core/data';
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
 * Page « Comptes rendus » : liste tabulaire des comptes rendus de la
 * communication (réunions, séminaires, conseils…) + création. Source : gateway
 * /api/communication/comptes-rendus. Tableau brandé, filtrable.
 *
 * Le backend communication-service n'expose que la création (POST) et la
 * publication (PATCH) : il n'y a ni PUT ni DELETE, donc on ne câble ni la
 * modification ni la suppression (pas de boutons qui échoueraient toujours).
 */
@Component({
  selector: 'app-comptes-rendus',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './comptes-rendus.component.html',
})
export class ComptesRendusComponent {
  private readonly svc = inject(CommunicationService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement réactif des comptes rendus (chargement / succès / erreur).
  protected readonly data = chargerDepuis(() => this.svc.listerComptesRendus());

  // Lignes du tableau (tableau vide tant que les données n'arrivent pas).
  protected readonly lignes = computed<CompteRendu[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<CompteRendu>[] = [
    { cle: 'title', libelle: 'Titre' },
    { cle: 'ownerName', libelle: 'Auteur' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      // Colonne courte (pastille) : largeur fixe pour ne pas voler l'espace au texte.
      largeur: '120px',
      ton: (c) => this.tonStatut(c.status),
    },
    // Colonne date courte : largeur fixe, les colonnes de texte long s'étirent.
    { cle: 'publishedAt', libelle: 'Publié le', type: 'date', largeur: '120px' },
    {
      cle: 'visibility',
      libelle: 'Visibilité',
      valeur: (c) => (c.visibility ?? []).join(', '),
    },
  ];

  // Champs du formulaire = sous-ensemble simple de CompteRenduCreationRequest.
  // visibility est obligatoire côté backend (Set de rôles, au moins un) : on le
  // saisit via un select mono-rôle, transformé ensuite en tableau à l'envoi.
  private readonly champs: ChampForm[] = [
    { cle: 'title', libelle: 'Titre', requis: true },
    {
      cle: 'type',
      libelle: 'Type de réunion',
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
    { cle: 'meetingDate', libelle: 'Date de la réunion', type: 'date', requis: true },
    { cle: 'body', libelle: 'Contenu', type: 'textarea' },
    {
      cle: 'visibility',
      libelle: 'Visibilité (rôle autorisé)',
      type: 'select',
      requis: true,
      options: [
        { valeur: 'admin', libelle: 'Administrateur' },
        { valeur: 'administratif', libelle: 'Administratif' },
        { valeur: 'enseignant', libelle: 'Enseignant' },
        { valeur: 'appui-insertion', libelle: "Appui à l'insertion" },
        { valeur: 'etudiant', libelle: 'Étudiant' },
      ],
    },
  ];

  /** Ouvre le drawer de création d'un compte rendu (brouillon). */
  protected nouveau(): void {
    this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre: 'Nouveau compte rendu', champs: this.champs })
      )
      .afterClosed()
      .subscribe((corps?: Record<string, unknown>) => {
        if (corps) {
          // authorId est requis par le DTO : UUID de l'utilisateur courant.
          // visibility doit être un ensemble de rôles : on enveloppe la sélection.
          const charge = {
            ...corps,
            authorId: this.auth.currentUser()?.id,
            visibility: corps['visibility'] ? [corps['visibility']] : [],
          };
          this.ecrire(this.svc.creerCompteRendu(charge), 'Compte rendu créé.');
        }
      });
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

  // Ton sémantique de la pastille selon le statut du compte rendu.
  private tonStatut(statut: string): StatusPillTon {
    switch (statut) {
      case 'publie':
        return 'succes';
      case 'valide':
        return 'info';
      case 'brouillon':
        return 'attention';
      case 'archive':
        return 'neutre';
      default:
        return 'neutre';
    }
  }
}
