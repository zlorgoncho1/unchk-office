import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { InsertionService, Partenaire } from '../../core/data';
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
 * Page « Partenaires » de l'appui à l'insertion — gestion complète (CRUD).
 * Liste filtrable + création / modification (drawer de formulaire) / suppression (confirmation).
 * Sert de référence pour les autres modules (même schéma : page-header + bouton Nouveau,
 * data-table avec actions Modifier/Supprimer, form-drawer, confirm-dialog, rechargement).
 */
@Component({
  selector: 'app-partenaires',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
    DataTableComponent,
    MatButtonModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './partenaires.component.html',
})
export class PartenairesComponent {
  private readonly svc = inject(InsertionService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Chargement des partenaires via le gateway.
  protected readonly data = chargerDepuis(() => this.svc.listerPartenaires());

  protected readonly lignes = computed<Partenaire[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau.
  protected readonly colonnes: ColonneTable<Partenaire>[] = [
    { cle: 'name', libelle: 'Nom' },
    { cle: 'sector', libelle: 'Secteur', valeur: (p) => p.sector },
    {
      cle: 'kind',
      libelle: 'Type',
      type: 'pastille',
      valeur: (p) => humaniser(p.kind),
      ton: () => 'info',
      largeur: '130px',
    },
    { cle: 'city', libelle: 'Ville', valeur: (p) => p.city, largeur: '140px' },
    { cle: 'contact', libelle: 'Contact', valeur: (p) => this.contact(p) },
  ];

  // Champs du formulaire = DTO backend PartnerRequest.
  private readonly champs: ChampForm[] = [
    { cle: 'name', libelle: 'Nom du partenaire', requis: true },
    {
      cle: 'kind',
      libelle: 'Type',
      type: 'select',
      requis: true,
      options: [
        { valeur: 'entreprise', libelle: 'Entreprise' },
        { valeur: 'administration', libelle: 'Administration' },
        { valeur: 'ong', libelle: 'ONG' },
        { valeur: 'institution', libelle: 'Institution' },
        { valeur: 'autre', libelle: 'Autre' },
      ],
    },
    { cle: 'sector', libelle: 'Secteur d’activité' },
    { cle: 'contactName', libelle: 'Nom du contact' },
    { cle: 'contactEmail', libelle: 'Courriel du contact', type: 'email' },
    { cle: 'contactPhone', libelle: 'Téléphone du contact', type: 'tel' },
    { cle: 'city', libelle: 'Ville' },
    { cle: 'address', libelle: 'Adresse', type: 'textarea' },
  ];

  /** Ouvre le drawer de création. */
  protected nouveau(): void {
    this.ouvrirForm('Nouveau partenaire').subscribe((corps) => {
      if (corps) {
        // active:true par défaut (la liste ne renvoie que les partenaires actifs).
        this.ecrire(this.svc.creerPartenaire({ ...corps, active: true }), 'Partenaire créé.');
      }
    });
  }

  /** Ouvre le drawer d'édition (pré-rempli). */
  protected onModifier(p: Partenaire): void {
    this.ouvrirForm('Modifier le partenaire', p).subscribe((corps) => {
      if (corps) {
        // On préserve l'état actif existant du partenaire lors de la modification.
        this.ecrire(
          this.svc.modifierPartenaire(p.id, { ...corps, active: p.active }),
          'Partenaire modifié.'
        );
      }
    });
  }

  /** Demande confirmation puis supprime le partenaire. */
  protected onSupprimer(p: Partenaire): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le partenaire',
          message: `Confirmer la suppression de « ${p.name} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimerPartenaire(p.id), 'Partenaire supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (création ou édition pré-remplie). */
  private ouvrirForm(
    titre: string,
    p?: Partenaire
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
          "Action impossible (droits insuffisants ou données invalides).",
          'OK',
          { duration: 4000 }
        ),
    });
  }

  /** Représentation lisible du contact d'un partenaire. */
  private contact(p: Partenaire): string | null {
    const detail = p.contactEmail ?? p.contactPhone;
    if (p.contactName && detail) {
      return `${p.contactName} — ${detail}`;
    }
    return p.contactName ?? detail ?? null;
  }
}
