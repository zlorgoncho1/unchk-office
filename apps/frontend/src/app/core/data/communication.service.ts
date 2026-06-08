import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CompteRendu, Reunion } from './api.models';

/**
 * Accès aux données « communication » (réunions, comptes rendus) via le gateway.
 * Routes réelles : /api/communication/reunions, /api/communication/comptes-rendus.
 */
@Injectable({ providedIn: 'root' })
export class CommunicationService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste des réunions (les plus récentes d'abord), renvoyée sous forme de tableau. */
  listerReunions(): Observable<Reunion[]> {
    return this.http.get<Reunion[]>(
      `${this.base}/api/communication/reunions`
    );
  }

  /** Liste des comptes rendus (les plus récents d'abord). */
  listerComptesRendus(): Observable<CompteRendu[]> {
    return this.http.get<CompteRendu[]>(
      `${this.base}/api/communication/comptes-rendus`
    );
  }
}
