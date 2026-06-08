import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  input,
} from '@angular/core';

/**
 * Carte de section brandée UNCHK.
 * Conteneur de contenu avec en-tête (titre + icône) optionnel et corps projeté.
 * Une zone projetée « [actions] » permet d'ajouter des actions à droite du titre.
 */
@Component({
  selector: 'unchk-section-card',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './section-card.component.html',
  styleUrl: './section-card.component.scss',
})
export class SectionCardComponent {
  // Titre de la section (optionnel : si absent, pas d'en-tête).
  readonly titre = input<string | null>(null);
  // Icône Solar optionnelle (sans le préfixe « solar: »).
  readonly icone = input<string | null>(null);
  // Retire le rembourrage interne du corps (utile pour les listes/tableaux).
  readonly sansBordure = input<boolean>(false);
}
