import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { Document, DocumentsService } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Documents ».
 * Liste paginée de la gestion documentaire (titre, catégorie, type, taille,
 * date, état d'archivage) présentée dans le tableau brandé filtrable.
 */
@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './documents.component.html',
})
export class DocumentsComponent {
  private readonly svc = inject(DocumentsService);

  // Ressource paginée : on charge la première page (50 éléments) via le gateway.
  protected readonly data = chargerDepuis(() => this.svc.lister(0, 50));

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
    },
    { cle: 'mimeType', libelle: 'Type' },
    { cle: 'sizeBytes', libelle: 'Taille', type: 'nombre' },
    { cle: 'createdAt', libelle: 'Date', type: 'date' },
    {
      cle: 'archived',
      libelle: 'État',
      type: 'pastille',
      // Libellé lisible selon l'archivage.
      valeur: (d) => (d.archived ? 'Archivé' : 'Actif'),
      ton: (d) => (d.archived ? 'succes' : 'neutre'),
    },
  ];
}
