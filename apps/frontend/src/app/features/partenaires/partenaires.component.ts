import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { InsertionService, Partenaire } from '../../core/data';
import {
  ColonneTable,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  DataTableComponent,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { humaniser } from '../../shared/util/format.util';

/**
 * Page « Partenaires » de l'appui à l'insertion.
 * Liste les partenaires (entreprises, administrations, ONG…) dans un tableau brandé
 * filtrable : nom, secteur, type (pastille), ville et contact.
 */
@Component({
  selector: 'app-partenaires',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
    DataTableComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './partenaires.component.html',
})
export class PartenairesComponent {
  private readonly svc = inject(InsertionService);

  // Chargement des partenaires via le gateway.
  protected readonly data = chargerDepuis(() => this.svc.listerPartenaires());

  // Lignes du tableau (liste simple Partenaire[]).
  protected readonly lignes = computed<Partenaire[]>(
    () => this.data.etat().donnees ?? []
  );

  // Colonnes du tableau des partenaires.
  protected readonly colonnes: ColonneTable<Partenaire>[] = [
    { cle: 'name', libelle: 'Nom' },
    {
      cle: 'sector',
      libelle: 'Secteur',
      // Secteur facultatif : tiret si absent (géré par le tableau).
      valeur: (p) => p.sector,
    },
    {
      cle: 'kind',
      libelle: 'Type',
      type: 'pastille',
      // Affiche le type humanisé (ex. « Ong » → libellé lisible).
      valeur: (p) => humaniser(p.kind),
      ton: () => 'info',
      // Colonne courte (pastille) : largeur fixe pour laisser respirer le reste.
      largeur: '130px',
    },
    {
      cle: 'city',
      libelle: 'Ville',
      valeur: (p) => p.city,
      // Colonne courte : largeur fixe.
      largeur: '140px',
    },
    {
      cle: 'contact',
      libelle: 'Contact',
      // Contact = nom puis email (fallback téléphone) si disponibles.
      valeur: (p) => this.contact(p),
    },
  ];

  /** Représentation lisible du contact d'un partenaire. */
  private contact(p: Partenaire): string | null {
    const detail = p.contactEmail ?? p.contactPhone;
    if (p.contactName && detail) {
      return `${p.contactName} — ${detail}`;
    }
    return p.contactName ?? detail ?? null;
  }
}
