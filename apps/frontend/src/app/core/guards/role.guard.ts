import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { Role } from '../models';

/**
 * Fabrique une garde par rôle.
 * Usage dans les routes : `canActivate: [roleGuard('admin', 'administratif')]`.
 *
 * - Non connecté -> redirige vers /login.
 * - Connecté mais sans rôle autorisé -> redirige vers /accueil (deny-by-default).
 */
export function roleGuard(...rolesAutorises: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url },
      });
    }

    if (auth.aUnRole(...rolesAutorises)) {
      return true;
    }

    // Connecté mais non autorisé : repli vers l'accueil.
    return router.createUrlTree(['/accueil']);
  };
}
