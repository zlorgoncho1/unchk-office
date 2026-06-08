import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  input,
} from '@angular/core';

/**
 * En-tête de page brandé UNCHK.
 * Titre, sous-titre optionnel et icône Solar ; une zone projetée (ng-content)
 * permet d'ajouter des actions à droite (boutons, filtres).
 */
@Component({
  selector: 'unchk-page-header',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
export class PageHeaderComponent {
  // Titre principal de la page.
  readonly titre = input.required<string>();
  // Sous-titre / description optionnelle.
  readonly sousTitre = input<string | null>(null);
  // Icône Solar optionnelle (sans le préfixe « solar: »).
  readonly icone = input<string | null>(null);
}
