import { MatDialogConfig } from '@angular/material/dialog';

/**
 * Construit la configuration d'ouverture d'un panneau latéral droit (drawer / slide-over),
 * cohérente avec le panelClass global « unchk-drawer » (cf. styles.scss).
 * À utiliser pour tous les drawers (détail, formulaire) afin d'uniformiser l'animation et le placement.
 *
 * @param data    données passées au composant du drawer
 * @param largeur largeur du panneau (défaut 480px)
 */
export function optionsDrawer<T>(data: T, largeur = '480px'): MatDialogConfig<T> {
  return {
    data,
    autoFocus: false,
    position: { top: '0', right: '0' },
    height: '100vh',
    width: largeur,
    maxWidth: '95vw',
    panelClass: 'unchk-drawer',
  };
}
