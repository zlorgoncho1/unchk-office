import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/** Jour récurrent d'un créneau (valeur JSON en minuscule, cf. JourSemaine.java). */
export type JourSemaine =
  | 'lundi'
  | 'mardi'
  | 'mercredi'
  | 'jeudi'
  | 'vendredi'
  | 'samedi'
  | 'dimanche';

/**
 * Créneau d'emploi du temps (miroir TypeScript de CreneauDto).
 * Un créneau est soit récurrent (dayOfWeek), soit ponctuel (sessionDate).
 */
export interface Creneau {
  id: string;
  formationId: string;
  courseLabel: string;
  formateurRef: string | null;
  formateurNom: string | null;
  dayOfWeek: JourSemaine | null;
  sessionDate: string | null;
  startTime: string; // HH:mm[:ss]
  endTime: string; // HH:mm[:ss]
  room: string | null;
}

/**
 * Accès aux données « emplois du temps » (créneaux d'une formation) via le gateway.
 * Routes réelles : /api/academic/formations/{formationId}/creneaux.
 */
@Injectable({ providedIn: 'root' })
export class EmploiTempsService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste les créneaux d'une formation. */
  listerCreneaux(formationId: string): Observable<Creneau[]> {
    return this.http.get<Creneau[]>(
      `${this.base}/api/academic/formations/${formationId}/creneaux`
    );
  }

  // --- Écritures (corps = CreneauCreationDto : courseLabel, formateurRef?,
  //     dayOfWeek?, sessionDate?, startTime, endTime, room?) ---

  /** Ajoute un créneau à l'emploi du temps d'une formation. */
  ajouterCreneau(
    formationId: string,
    corps: Record<string, unknown>
  ): Observable<Creneau> {
    return this.http.post<Creneau>(
      `${this.base}/api/academic/formations/${formationId}/creneaux`,
      corps
    );
  }

  /** Supprime un créneau de l'emploi du temps d'une formation. */
  supprimerCreneau(
    formationId: string,
    creneauId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/api/academic/formations/${formationId}/creneaux/${creneauId}`
    );
  }
}
