import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthTokens,
  LoginRequest,
  RefreshRequest,
  Role,
  User,
} from '../models';
import { TokenStorageService } from './token-storage.service';
import { decoderJwt, jwtExpire } from './jwt.util';

// Endpoints d'authentification (préfixe routé par le gateway).
const URL_LOGIN = '/api/identity/auth/login';
const URL_REFRESH = '/api/identity/auth/refresh';
const URL_LOGOUT = '/api/identity/auth/logout';

/**
 * Service d'authentification.
 * <p>
 * - Connexion / déconnexion / rafraîchissement auprès du gateway.
 * - Stockage des jetons (via TokenStorageService).
 * - Décodage du JWT pour reconstruire l'utilisateur courant et ses rôles.
 * - Expose un signal `currentUser` et des dérivés (isAuthenticated, roles).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly stockage = inject(TokenStorageService);
  private readonly base = environment.apiBaseUrl;

  // Utilisateur courant (null si non connecté). Source réactive de l'app.
  private readonly _currentUser = signal<User | null>(this.utilisateurDepuisStockage());
  readonly currentUser = this._currentUser.asReadonly();

  // Vrai si un access token valide (non expiré) est présent.
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  // Rôles de l'utilisateur courant (liste vide si déconnecté).
  readonly roles = computed<Role[]>(() => this._currentUser()?.roles ?? []);

  /** Connexion : POST /login puis mémorisation des jetons et de l'utilisateur. */
  login(identifiants: LoginRequest): Observable<AuthTokens> {
    return this.http
      .post<AuthTokens>(`${this.base}${URL_LOGIN}`, identifiants)
      .pipe(tap((jetons) => this.appliquerJetons(jetons)));
  }

  /** Rafraîchissement : échange le refresh token contre un nouveau couple de jetons. */
  refresh(): Observable<AuthTokens> {
    const corps: RefreshRequest = {
      refreshToken: this.stockage.getRefreshToken() ?? '',
    };
    return this.http
      .post<AuthTokens>(`${this.base}${URL_REFRESH}`, corps)
      .pipe(tap((jetons) => this.appliquerJetons(jetons)));
  }

  /**
   * Déconnexion : tente de révoquer le refresh token côté serveur, puis
   * efface l'état local quoi qu'il arrive (best-effort).
   */
  logout(): void {
    const refreshToken = this.stockage.getRefreshToken();
    if (refreshToken) {
      const corps: RefreshRequest = { refreshToken };
      // Best-effort : on ignore l'issue de l'appel réseau.
      this.http.post<void>(`${this.base}${URL_LOGOUT}`, corps).subscribe({
        error: () => undefined,
      });
    }
    this.effacerSession();
  }

  /** Efface les jetons et l'utilisateur courant (sans appel réseau). */
  effacerSession(): void {
    this.stockage.clear();
    this._currentUser.set(null);
  }

  /** Indique si l'utilisateur possède au moins un des rôles demandés. */
  aUnRole(...roles: Role[]): boolean {
    const miens = this.roles();
    return roles.some((r) => miens.includes(r));
  }

  /** Access token courant (pour l'intercepteur / le WebSocket). */
  getAccessToken(): string | null {
    return this.stockage.getAccessToken();
  }

  // --- Interne ---

  // Enregistre les jetons et met à jour l'utilisateur courant à partir du JWT.
  private appliquerJetons(jetons: AuthTokens): void {
    this.stockage.setTokens(jetons.accessToken, jetons.refreshToken);
    this._currentUser.set(this.utilisateurDepuisToken(jetons.accessToken));
  }

  // Reconstruit l'utilisateur depuis le jeton présent dans le stockage (init).
  private utilisateurDepuisStockage(): User | null {
    const token = this.stockage.getAccessToken();
    return token ? this.utilisateurDepuisToken(token) : null;
  }

  // Décode un access token -> User. Retourne null si invalide ou expiré.
  private utilisateurDepuisToken(token: string): User | null {
    const payload = decoderJwt(token);
    if (!payload || jwtExpire(payload)) {
      return null;
    }
    return {
      id: payload.sub,
      email: payload.email,
      fullName: payload.name,
      roles: payload.roles,
    };
  }
}
