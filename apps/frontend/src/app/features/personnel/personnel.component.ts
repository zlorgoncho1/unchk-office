import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { PeopleService } from '../../core/data';
import { Personnel } from '../../core/data/api.models';
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
 * Page « Personnel » : liste paginée du personnel de l'université.
 * Tableau brandé filtrable (matricule, identité, catégorie, grade, département…).
 */
@Component({
  selector: 'app-personnel',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './personnel.component.html',
})
export class PersonnelComponent {
  private readonly people = inject(PeopleService);

  // Chargement de la première page (50 agents) via le gateway.
  protected readonly data = chargerDepuis(() => this.people.listerPersonnel(0, 50));

  // Lignes du tableau : contenu de la page (PageReponse<Personnel>).
  protected readonly lignes = computed<Personnel[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Colonnes du tableau brandé.
  protected readonly colonnes: ColonneTable<Personnel>[] = [
    { cle: 'matricule', libelle: 'Matricule' },
    { cle: 'lastName', libelle: 'Nom' },
    { cle: 'firstName', libelle: 'Prénom' },
    {
      cle: 'kind',
      libelle: 'Catégorie',
      type: 'pastille',
      // Catégorie humanisée (ex. « enseignant_associe » → « Enseignant associé »).
      valeur: (p) => humaniser(p.kind),
      ton: () => 'info',
    },
    { cle: 'grade', libelle: 'Grade' },
    { cle: 'department', libelle: 'Département' },
    { cle: 'speciality', libelle: 'Spécialité' },
    {
      cle: 'active',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (p) => (p.active ? 'Actif' : 'Inactif'),
      ton: (p) => (p.active ? 'succes' : 'neutre'),
    },
  ];
}
