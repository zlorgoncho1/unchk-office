import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Formation } from './api.models';

/**
 * Accès aux données « academic » (formations) via le gateway.
 * Route réelle : /api/academic/formations.
 */
@Injectable({ providedIn: 'root' })
export class AcademicService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste des formations (le endpoint renvoie un tableau, pas une page). */
  listerFormations(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.base}/api/academic/formations`);
  }

  /** Export PDF des statistiques de formations par genre (fichier binaire, JWT par l'intercepteur). */
  exporterFormationsPdf(): Observable<Blob> {
    return this.http.get(`${this.base}/api/academic/statistiques/formations.pdf`, {
      responseType: 'blob',
    });
  }

  /** Export Excel (xlsx) des statistiques de formations par genre (fichier binaire, JWT par l'intercepteur). */
  exporterFormationsXlsx(): Observable<Blob> {
    return this.http.get(`${this.base}/api/academic/statistiques/formations.xlsx`, {
      responseType: 'blob',
    });
  }

  // --- CRUD formations (corps = FormationCreationDto / FormationMajDto :
  //     code, label, level, kind, funding, startDate, endDate,
  //     trainedMale, trainedFemale, responsibleRef[, active à la modification]) ---

  /** Crée une formation. */
  creerFormation(corps: Record<string, unknown>): Observable<Formation> {
    return this.http.post<Formation>(`${this.base}/api/academic/formations`, corps);
  }

  /** Modifie une formation existante. */
  modifierFormation(id: string, corps: Record<string, unknown>): Observable<Formation> {
    return this.http.put<Formation>(`${this.base}/api/academic/formations/${id}`, corps);
  }

  /** Supprime une formation. */
  supprimerFormation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/academic/formations/${id}`);
  }
}
