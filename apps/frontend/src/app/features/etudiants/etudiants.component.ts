import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core';

import { Etudiant, PeopleService } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillTon,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';

/**
 * Page « Étudiants » : liste paginée des étudiants inscrits.
 * Tableau brandé filtrable (matricule, identité, promo, statut) avec pastilles
 * de genre et de statut, dans le respect de la charte UNCHK.
 */
@Component({
  selector: 'app-etudiants',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './etudiants.component.html',
  styleUrl: './etudiants.component.scss',
})
export class EtudiantsComponent {
  private readonly people = inject(PeopleService);

  // Chargement de la première page d'étudiants (50 lignes).
  protected readonly data = chargerDepuis(() => this.people.listerEtudiants(0, 50));

  // Lignes du tableau : contenu de la page paginée (ou liste vide).
  protected readonly lignes = computed<Etudiant[]>(
    () => this.data.etat().donnees?.content ?? []
  );

  // Colonnes du tableau, alignées sur les champs du DTO Etudiant.
  protected readonly colonnes: ColonneTable<Etudiant>[] = [
    { cle: 'matricule', libelle: 'Matricule' },
    { cle: 'lastName', libelle: 'Nom' },
    { cle: 'firstName', libelle: 'Prénom' },
    {
      cle: 'gender',
      libelle: 'Genre',
      type: 'pastille',
      align: 'centre',
      // On affiche un libellé lisible plutôt que le code brut (M/F).
      valeur: (e) => this.libelleGenre(e.gender),
      ton: (e) => this.tonGenre(e.gender),
    },
    { cle: 'promotion', libelle: 'Promo' },
    { cle: 'enrollmentYear', libelle: 'Année', type: 'nombre' },
    {
      cle: 'status',
      libelle: 'Statut',
      type: 'pastille',
      align: 'centre',
      ton: (e) => this.tonStatut(e.status),
    },
  ];

  // Libellé lisible du genre à partir du code stocké.
  private libelleGenre(genre: string): string {
    if (genre === 'M') {
      return 'Masculin';
    }
    if (genre === 'F') {
      return 'Féminin';
    }
    return genre || '—';
  }

  // Ton de la pastille de genre (purement décoratif, ton info/danger doux).
  private tonGenre(genre: string): StatusPillTon {
    if (genre === 'M') {
      return 'info';
    }
    if (genre === 'F') {
      return 'danger';
    }
    return 'neutre';
  }

  // Ton de la pastille de statut selon l'état de l'étudiant.
  private tonStatut(statut: string): StatusPillTon {
    switch (statut) {
      case 'inscrit':
        return 'succes';
      case 'diplome':
        return 'info';
      case 'suspendu':
        return 'attention';
      case 'abandon':
        return 'danger';
      default:
        return 'neutre';
    }
  }
}
