import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import {
  BehaviorSubject,
  Observable,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { AuthTokens } from '../models';

// Chemins d'authentification à NE PAS intercepter pour le refresh
// (sinon boucle infinie sur un 401 du login / refresh lui-même).
const CHEMINS_AUTH = ['/api/identity/auth/login', '/api/identity/auth/refresh'];

// État partagé du rafraîchissement en cours (évite les refresh concurrents).
let rafraichissementEnCours = false;
const nouveauToken$ = new BehaviorSubject<string | null>(null);

/**
 * Intercepteur fonctionnel :
 * - attache l'en-tête `Authorization: Bearer <accessToken>` aux appels API ;
 * - sur 401, tente UN rafraîchissement puis rejoue la requête ;
 * - en cas d'échec du refresh, déconnecte et redirige vers /login.
 */
export const authInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // On n'ajoute le jeton qu'aux appels d'API (pas aux ressources statiques externes).
  const token = auth.getAccessToken();
  const requeteAuth = token && !estCheminAuth(requete.url)
    ? ajouterBearer(requete, token)
    : requete;

  return suivant(requeteAuth).pipe(
    catchError((erreur: unknown) => {
      const est401 =
        erreur instanceof HttpErrorResponse && erreur.status === 401;

      // Pas un 401, ou requête d'auth elle-même : on propage l'erreur.
      if (!est401 || estCheminAuth(requete.url)) {
        return throwError(() => erreur);
      }

      // Aucun refresh token disponible : déconnexion immédiate.
      if (!auth.getAccessToken()) {
        return rejeterEtRediriger(auth, router, erreur);
      }

      return gerer401(requete, suivant, auth, router, erreur);
    })
  );
};

// Gère un 401 : déclenche (ou attend) un rafraîchissement, puis rejoue la requête.
function gerer401(
  requete: HttpRequest<unknown>,
  suivant: HttpHandlerFn,
  auth: AuthService,
  router: Router,
  erreurInitiale: unknown
): Observable<HttpEvent<unknown>> {
  if (rafraichissementEnCours) {
    // Un refresh est déjà en cours : on attend le nouveau jeton puis on rejoue.
    return nouveauToken$.pipe(
      filter((t): t is string => t !== null),
      take(1),
      switchMap((t) => suivant(ajouterBearer(requete, t)))
    );
  }

  rafraichissementEnCours = true;
  nouveauToken$.next(null);

  return auth.refresh().pipe(
    switchMap((jetons: AuthTokens) => {
      rafraichissementEnCours = false;
      nouveauToken$.next(jetons.accessToken);
      // Rejoue la requête d'origine avec le nouvel access token.
      return suivant(ajouterBearer(requete, jetons.accessToken));
    }),
    catchError((erreurRefresh: unknown) => {
      rafraichissementEnCours = false;
      // Le refresh a échoué : session expirée, on déconnecte et redirige.
      return rejeterEtRediriger(auth, router, erreurRefresh ?? erreurInitiale);
    })
  );
}

// Déconnecte localement et redirige vers la page de connexion.
function rejeterEtRediriger(
  auth: AuthService,
  router: Router,
  erreur: unknown
): Observable<never> {
  auth.effacerSession();
  void router.navigate(['/login']);
  return throwError(() => erreur);
}

// Clone la requête en y ajoutant l'en-tête Authorization.
function ajouterBearer(
  requete: HttpRequest<unknown>,
  token: string
): HttpRequest<unknown> {
  return requete.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}

// Vrai si l'URL cible un endpoint d'authentification (login/refresh).
function estCheminAuth(url: string): boolean {
  return CHEMINS_AUTH.some((chemin) => url.includes(chemin));
}
