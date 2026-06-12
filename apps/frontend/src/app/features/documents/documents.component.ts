import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { Document, DocumentsService } from '../../core/data';
import { CATEGORIES_DOCUMENT } from './categories-document';
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
import { chargerDepuis } from '../../shared/util/loadable';
import { formaterTaille, humaniser } from '../../shared/util/format.util';
import {
  UploadDocumentDialogComponent,
  UploadDocumentResultat,
} from './upload-document-dialog.component';

/**
 * Page « Documents ».
 * Liste paginée de la gestion documentaire (titre, catégorie, type, taille, date, archivage)
 * dans le tableau brandé, avec modification des métadonnées (drawer) et suppression (confirmation).
 *
 * NB : la CRÉATION d'un document est multipart côté backend (métadonnées + fichier binaire
 * déposé sur MinIO via @RequestPart("file")). Le form-drawer générique ne gère pas la sélection
 * d'un fichier ; on passe donc par un dialog dédié (UploadDocumentDialogComponent) ouvert en
 * panneau latéral pour le DÉPÔT. On couvre aussi la MODIFICATION des métadonnées (PATCH : titre,
 * description, archivage, visibilité) et la SUPPRESSION (DELETE).
 */
@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './documents.component.html',
  styles: [
    `
      /* Sélecteur de catégorie compact dans l'en-tête de la carte. */
      .doc-filtre-categorie {
        width: 220px;
        margin: 0;
      }
      /* Le mat-form-field « outline » réserve de la place sous le champ : on la retire ici. */
      .doc-filtre-categorie ::ng-deep .mat-mdc-form-field-subscript-wrapper {
        display: none;
      }
    `,
  ],
})
export class DocumentsComponent {
  private readonly svc = inject(DocumentsService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  // Catégories proposées au filtre (« » = toutes les catégories).
  protected readonly categories = CATEGORIES_DOCUMENT;

  // Catégorie sélectionnée dans le filtre ('' = toutes). Pilote l'appel filtré.
  protected readonly categorieFiltre = signal<string>('');

  // Ressource paginée : on charge la première page (50 éléments) via le gateway,
  // filtrée par la catégorie courante ('' -> liste complète).
  protected readonly data = chargerDepuis(() =>
    this.svc.lister(0, 50, this.categorieFiltre() || undefined)
  );

  // Lignes du tableau : contenu de la page (vide si indisponible).
  protected readonly lignes = computed<Document[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Description des colonnes du tableau documentaire.
  protected readonly colonnes: ColonneTable<Document>[] = [
    { cle: 'title', libelle: 'Titre' },
    {
      cle: 'category',
      libelle: 'Catégorie',
      type: 'pastille',
      // Catégorie humanisée (underscores -> espaces, capitale initiale).
      valeur: (d) => humaniser(d.category),
      ton: () => 'info',
      // Colonne courte : largeur figée pour laisser respirer le titre.
      largeur: '140px',
    },
    { cle: 'mimeType', libelle: 'Type' },
    // Taille lisible (Ko/Mo) plutôt que des octets bruts ; alignée à droite.
    {
      cle: 'sizeBytes',
      libelle: 'Taille',
      align: 'droite',
      valeur: (d) => formaterTaille(d.sizeBytes),
      largeur: '110px',
    },
    { cle: 'createdAt', libelle: 'Date', type: 'date', largeur: '110px' },
    {
      cle: 'archived',
      libelle: 'État',
      type: 'pastille',
      // Libellé lisible selon l'archivage.
      valeur: (d) => (d.archived ? 'Archivé' : 'Actif'),
      ton: (d) => (d.archived ? 'succes' : 'neutre'),
      // Colonne courte : largeur figée (pastille Actif/Archivé).
      largeur: '110px',
    },
  ];

  // Champs du formulaire d'édition des métadonnées = DTO MettreAJourDocumentRequete.
  // (Le binaire reste figé au dépôt ; la catégorie est reclassable.)
  private readonly champs: ChampForm[] = [
    { cle: 'title', libelle: 'Titre', requis: true, largeur: 'pleine' },
    {
      cle: 'category',
      libelle: 'Catégorie',
      type: 'select',
      // Mêmes options que le filtre / le dépôt (codes alignés sur le backend).
      options: CATEGORIES_DOCUMENT.map((c) => ({ valeur: c.valeur, libelle: c.libelle })),
    },
    { cle: 'description', libelle: 'Description', type: 'textarea', largeur: 'pleine' },
    {
      cle: 'archived',
      libelle: 'État',
      type: 'select',
      options: [
        { valeur: 'false', libelle: 'Actif' },
        { valeur: 'true', libelle: 'Archivé' },
      ],
    },
    {
      cle: 'visibility',
      libelle: 'Visibilité (rôle)',
      type: 'select',
      options: [
        { valeur: 'admin', libelle: 'Administrateur' },
        { valeur: 'administratif', libelle: 'Administratif' },
        { valeur: 'enseignant', libelle: 'Enseignant' },
        { valeur: 'appui-insertion', libelle: 'Appui à l’insertion' },
        { valeur: 'etudiant', libelle: 'Étudiant' },
      ],
      aide: 'Rôle autorisé à voir le document.',
    },
  ];

  /**
   * Change la catégorie filtrée puis recharge la table.
   * Une valeur vide ('') retire le filtre (toutes les catégories).
   */
  protected onFiltrerCategorie(categorie: string): void {
    this.categorieFiltre.set(categorie);
    // La source de chargement relit le signal : on relance simplement la requête.
    this.data.recharger();
  }

  /**
   * Ouvre le dialog de dépôt (upload) en panneau latéral, puis envoie le multipart.
   * La sélection d'un fichier n'étant pas gérée par le form-drawer générique, on passe par
   * un dialog dédié qui renvoie les métadonnées + le fichier choisi.
   */
  protected nouveau(): void {
    this.dialog
      .open(
        UploadDocumentDialogComponent,
        optionsDrawer<undefined>(undefined)
      )
      .afterClosed()
      .subscribe((res: UploadDocumentResultat | undefined) => {
        if (res) {
          this.ecrire(
            this.svc.creerDocument(res.meta, res.fichier),
            'Document déposé.'
          );
        }
      });
  }

  /** Ouvre le drawer d'édition des métadonnées (pré-rempli). */
  protected onModifier(d: Document): void {
    // Valeurs initiales adaptées aux champs select (booléen -> chaîne, liste -> 1er rôle).
    const initial: Record<string, unknown> = {
      title: d.title,
      category: d.category,
      description: d.description,
      archived: d.archived ? 'true' : 'false',
      visibility: d.visibility?.[0],
    };
    this.ouvrirForm('Modifier le document', initial).subscribe((corps) => {
      if (corps) {
        // On normalise le corps pour le DTO : archived booléen, visibility en liste.
        const charge: Record<string, unknown> = {
          title: corps['title'],
          category: corps['category'],
          description: corps['description'],
          archived: corps['archived'] === 'true',
        };
        // La visibilité n'est transmise que si un rôle est choisi (sinon on garde l'existant).
        if (corps['visibility']) {
          charge['visibility'] = [corps['visibility']];
        }
        this.ecrire(this.svc.modifier(d.id, charge), 'Document modifié.');
      }
    });
  }

  /** Demande confirmation puis supprime le document. */
  protected onSupprimer(d: Document): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titre: 'Supprimer le document',
          message: `Confirmer la suppression de « ${d.title} » ? Cette action est définitive.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.ecrire(this.svc.supprimer(d.id), 'Document supprimé.');
        }
      });
  }

  /** Ouvre le drawer de formulaire (édition des métadonnées, pré-rempli). */
  private ouvrirForm(
    titre: string,
    valeurInitiale?: Record<string, unknown>
  ): Observable<Record<string, unknown> | undefined> {
    return this.dialog
      .open(
        FormDrawerComponent,
        optionsDrawer({ titre, champs: this.champs, valeurInitiale })
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
          'Action impossible (droits insuffisants ou données invalides).',
          'OK',
          { duration: 4000 }
        ),
    });
  }
}
