import { CUSTOM_ELEMENTS_SCHEMA, Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Indicateur de chargement brandé UNCHK.
 * Spinner Material centré avec un message optionnel.
 * Utilisé pour l'état « chargement » des dashboards en attente des données.
 */
@Component({
  selector: 'unchk-loading-state',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './loading-state.component.html',
  styleUrl: './loading-state.component.scss',
})
export class LoadingStateComponent {
  // Message affiché sous le spinner.
  readonly message = input<string>('Chargement…');
  // Version compacte (panneaux étroits).
  readonly compact = input<boolean>(false);
}
