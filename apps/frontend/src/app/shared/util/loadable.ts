import { Signal, signal } from '@angular/core';
import { Observable } from 'rxjs';

// État d'une ressource asynchrone (chargement / succès / erreur).
export interface EtatLoadable<T> {
  chargement: boolean;
  erreur: string | null;
  donnees: T | null;
}

// Ressource chargeable exposée aux composants (signal en lecture seule + relance).
export interface Loadable<T> {
  etat: Signal<EtatLoadable<T>>;
  recharger: () => void;
}

/**
 * Transforme un Observable (un appel HTTP) en ressource réactive à base de signaux.
 * Gère proprement les trois états : chargement, succès, erreur.
 * `recharger()` relance la source (utile pour un bouton « Réessayer »).
 */
export function chargerDepuis<T>(source: () => Observable<T>): Loadable<T> {
  const etat = signal<EtatLoadable<T>>({
    chargement: true,
    erreur: null,
    donnees: null,
  });

  const lancer = (): void => {
    etat.set({ chargement: true, erreur: null, donnees: null });
    source().subscribe({
      next: (donnees) => etat.set({ chargement: false, erreur: null, donnees }),
      error: (e) =>
        etat.set({
          chargement: false,
          erreur: messageErreur(e),
          donnees: null,
        }),
    });
  };

  lancer();

  return { etat: etat.asReadonly(), recharger: lancer };
}

// Traduit une erreur HTTP en message lisible (français), sans fuite technique.
function messageErreur(e: unknown): string {
  const err = e as { status?: number };
  if (err?.status === 401 || err?.status === 403) {
    return "Accès refusé : vos droits ne permettent pas d'afficher ces données.";
  }
  if (err?.status === 404) {
    return 'Données introuvables.';
  }
  if (err?.status === 0) {
    return 'Service indisponible : impossible de joindre le serveur.';
  }
  return 'Une erreur est survenue lors du chargement des données.';
}
