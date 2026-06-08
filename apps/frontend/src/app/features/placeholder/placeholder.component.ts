import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { PageHeaderComponent, EmptyStateComponent } from '../../shared/ui';

/**
 * Page générique « bientôt disponible ».
 * Utilisée comme cible des liens de navigation dont le module métier
 * sera développé aux étapes suivantes. Le titre et l'icône proviennent
 * de la donnée de route (`data.titre`, `data.icone`).
 */
@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [PageHeaderComponent, EmptyStateComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './placeholder.component.html',
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  // Métadonnées de la route (titre + icône) sous forme de signal.
  readonly meta = toSignal(
    this.route.data.pipe(
      map((d) => ({
        titre: (d['titre'] as string) ?? 'Module',
        icone: (d['icone'] as string) ?? 'widget-bold-duotone',
      }))
    ),
    { initialValue: { titre: 'Module', icone: 'widget-bold-duotone' } }
  );
}
