import {
  CUSTOM_ELEMENTS_SCHEMA,
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';

import { AcademicService, Formation } from '../../core/data';
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
 * Page « Formations » : liste tabulaire des formations de l'université.
 * Données via AcademicService.listerFormations() (tableau brut).
 * Affiche niveau/statut en pastilles et le nombre total de formés (H + F).
 */
@Component({
  selector: 'app-formations',
  standalone: true,
  imports: [
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
    DataTableComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formations.component.html',
})
export class FormationsComponent {
  private readonly svc = inject(AcademicService);

  // Chargement des formations (le endpoint renvoie un tableau, pas une page).
  protected readonly data = chargerDepuis(() => this.svc.listerFormations());
  protected readonly lignes = computed(() => this.data.etat().donnees ?? []);

  // Description des colonnes du tableau brandé.
  // Les colonnes courtes ont une largeur fixe pour laisser respirer
  // les colonnes de texte long (Intitulé, Type, Financement).
  protected readonly colonnes: ColonneTable<Formation>[] = [
    { cle: 'code', libelle: 'Code', largeur: '110px' },
    { cle: 'label', libelle: 'Intitulé' },
    {
      cle: 'level',
      libelle: 'Niveau',
      type: 'pastille',
      valeur: (f) => humaniser(f.level),
      ton: () => 'info',
      largeur: '120px',
    },
    { cle: 'kind', libelle: 'Type', valeur: (f) => humaniser(f.kind) },
    {
      cle: 'funding',
      libelle: 'Financement',
      valeur: (f) => humaniser(f.funding),
    },
    {
      cle: 'formes',
      libelle: 'Formés',
      type: 'nombre',
      valeur: (f) => f.trainedMale + f.trainedFemale,
      largeur: '90px',
    },
    { cle: 'startDate', libelle: 'Début', type: 'date', largeur: '110px' },
    { cle: 'endDate', libelle: 'Fin', type: 'date', largeur: '110px' },
    {
      cle: 'active',
      libelle: 'Statut',
      type: 'pastille',
      valeur: (f) => (f.active ? 'Active' : 'Inactive'),
      ton: (f) => (f.active ? 'succes' : 'neutre'),
      largeur: '110px',
    },
  ];
}
