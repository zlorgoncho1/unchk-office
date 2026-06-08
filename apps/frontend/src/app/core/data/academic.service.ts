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
}
