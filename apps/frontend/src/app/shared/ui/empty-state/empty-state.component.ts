import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  input,
} from '@angular/core';

/**
 * État vide brandé UNCHK.
 * Affiche une icône, un titre et un message lorsqu'aucune donnée n'est
 * disponible. Une zone projetée (ng-content) permet d'ajouter une action.
 */
@Component({
  selector: 'unchk-empty-state',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
})
export class EmptyStateComponent {
  // Icône Solar (sans le préfixe « solar: »).
  readonly icone = input<string>('inbox-line-duotone');
  // Titre de l'état vide.
  readonly titre = input.required<string>();
  // Message d'explication optionnel.
  readonly message = input<string | null>(null);
  // Taille compacte (pour les petits panneaux comme le rail droit).
  readonly compact = input<boolean>(false);
}
