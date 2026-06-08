import { Injectable } from '@angular/core';

// Clés de stockage des jetons dans le localStorage.
const CLE_ACCESS = 'unchk.accessToken';
const CLE_REFRESH = 'unchk.refreshToken';

/**
 * Stockage des jetons d'authentification (localStorage).
 * Isolé dans un service dédié pour centraliser l'accès au stockage
 * et faciliter un éventuel changement de stratégie (sessionStorage, cookie…).
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  // Lit l'access token courant (ou null).
  getAccessToken(): string | null {
    return this.lire(CLE_ACCESS);
  }

  // Lit le refresh token courant (ou null).
  getRefreshToken(): string | null {
    return this.lire(CLE_REFRESH);
  }

  // Enregistre le couple de jetons.
  setTokens(accessToken: string, refreshToken: string): void {
    this.ecrire(CLE_ACCESS, accessToken);
    this.ecrire(CLE_REFRESH, refreshToken);
  }

  // Efface les jetons (déconnexion).
  clear(): void {
    this.supprimer(CLE_ACCESS);
    this.supprimer(CLE_REFRESH);
  }

  // --- Accès bas niveau, protégés contre l'absence de localStorage (SSR/tests) ---
  private lire(cle: string): string | null {
    try {
      return localStorage.getItem(cle);
    } catch {
      return null;
    }
  }

  private ecrire(cle: string, valeur: string): void {
    try {
      localStorage.setItem(cle, valeur);
    } catch {
      // Stockage indisponible : on ignore silencieusement.
    }
  }

  private supprimer(cle: string): void {
    try {
      localStorage.removeItem(cle);
    } catch {
      // Stockage indisponible : on ignore silencieusement.
    }
  }
}
