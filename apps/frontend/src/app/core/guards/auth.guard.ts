import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/**
 * Garde d'authentification : autorise l'accès si l'utilisateur est connecté,
 * sinon redirige vers /login en mémorisant l'URL demandée (returnUrl).
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  // Non connecté : redirection vers la connexion avec l'URL de retour.
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url },
  });
};
