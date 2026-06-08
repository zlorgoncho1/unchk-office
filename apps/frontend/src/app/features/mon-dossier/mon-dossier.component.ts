import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';

import { DiplomeDto, Etudiant, PeopleService } from '../../core/data';
import {
  ColonneTable,
  DataTableComponent,
  EmptyStateComponent,
  LoadingStateComponent,
  PageHeaderComponent,
  SectionCardComponent,
  StatusPillComponent,
  StatusPillTon,
} from '../../shared/ui';
import { chargerDepuis } from '../../shared/util/loadable';
import { formaterDate, humaniser } from '../../shared/util/format.util';

/**
 * Page « Mon dossier » : fiche de l'étudiant connecté.
 * Source : PeopleService.maFiche() -> Etudiant (404 si le compte n'est rattaché
 * à aucune fiche : on bascule alors sur un état vide via data.etat().erreur).
 * Affiche les informations en paires clé/valeur (style .fiche partagé) et la
 * liste des diplômes obtenus dans un tableau brandé.
 */
@Component({
  selector: 'app-mon-dossier',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    DataTableComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    StatusPillComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './mon-dossier.component.html',
  // Réutilise les styles partagés des tableaux de bord (.fiche, .home__section)
  // et complète avec quelques retouches propres à cette page.
  styleUrls: ['../home/home-shared.scss', './mon-dossier.component.scss'],
})
export class MonDossierComponent {
  private readonly people = inject(PeopleService);

  // Fiche de l'étudiant connecté (peut échouer en 404 si non rattachée).
  protected readonly fiche = chargerDepuis(() => this.people.maFiche());

  // Raccourci vers la fiche chargée (ou null).
  protected readonly etudiant = computed<Etudiant | null>(
    () => this.fiche.etat().donnees ?? null
  );

  // Diplômes obtenus (liste éventuellement vide).
  protected readonly diplomes = computed<DiplomeDto[]>(
    () => this.etudiant()?.diplomas ?? []
  );

  // Fonctions de formatage exposées au gabarit.
  protected readonly exposeDate = formaterDate;
  protected readonly exposeHumaniser = humaniser;

  // Colonnes du tableau des diplômes.
  protected readonly colonnesDiplomes: ColonneTable<DiplomeDto>[] = [
    { cle: 'label', libelle: 'Diplôme' },
    { cle: 'level', libelle: 'Niveau' },
    { cle: 'obtainedAt', libelle: 'Obtenu le', type: 'date', align: 'droite' },
  ];

  // Genre lisible (M/F -> Homme/Femme, sinon valeur brute humanisée).
  protected genreLisible(genre: string | null | undefined): string {
    switch (genre) {
      case 'M':
        return 'Homme';
      case 'F':
        return 'Femme';
      default:
        return humaniser(genre);
    }
  }

  // Ton sémantique de la pastille selon le statut de l'étudiant.
  protected tonStatut(): StatusPillTon {
    switch (this.etudiant()?.status) {
      case 'inscrit':
        return 'info';
      case 'diplome':
        return 'succes';
      case 'suspendu':
        return 'attention';
      case 'abandon':
        return 'danger';
      default:
        return 'neutre';
    }
  }

  // Plage d'années « début → sortie » (— si rien à afficher).
  protected anneesScolarite(): string {
    const e = this.etudiant();
    if (!e?.enrollmentYear && !e?.exitYear) {
      return '—';
    }
    const debut = e?.enrollmentYear ?? '…';
    const fin = e?.exitYear ?? 'en cours';
    return `${debut} → ${fin}`;
  }
}
