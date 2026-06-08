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

  // --- Écriture réunions (corps = ReunionCreationRequest : title, type, startsAt,
  //     endsAt, location, organizerId) ---
  // Note : le backend n'expose que la création (POST). Pas de PUT/DELETE côté
  //   communication-service, donc pas de modification ni de suppression ici.

  /** Planifie (crée) une réunion. */
  creerReunion(corps: Record<string, unknown>): Observable<Reunion> {
    return this.http.post<Reunion>(
      `${this.base}/api/communication/reunions`,
      corps
    );
  }

  // --- Écriture comptes rendus (corps = CompteRenduCreationRequest : title, type,
  //     meetingDate, authorId, visibility) ---
  // Note : le backend n'expose que la création (POST) et la publication (PATCH).
  //   Pas de PUT/DELETE, donc pas de modification ni de suppression ici.

  /** Rédige (crée) un compte rendu en brouillon. */
  creerCompteRendu(corps: Record<string, unknown>): Observable<CompteRendu> {
    return this.http.post<CompteRendu>(
      `${this.base}/api/communication/comptes-rendus`,
      corps
    );
  }
}
