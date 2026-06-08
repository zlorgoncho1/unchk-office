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

  /** Planifie (crée) une réunion. */
  creerReunion(corps: Record<string, unknown>): Observable<Reunion> {
    return this.http.post<Reunion>(`${this.base}/api/communication/reunions`, corps);
  }

  /** Modifie une réunion. */
  modifierReunion(id: string, corps: Record<string, unknown>): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.base}/api/communication/reunions/${id}`, corps);
  }

  /** Supprime une réunion. */
  supprimerReunion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/communication/reunions/${id}`);
  }

  // --- Écriture comptes rendus (corps = CompteRenduCreationRequest : title, type,
  //     meetingDate, authorId, visibility) ---

  /** Rédige (crée) un compte rendu en brouillon. */
  creerCompteRendu(corps: Record<string, unknown>): Observable<CompteRendu> {
    return this.http.post<CompteRendu>(`${this.base}/api/communication/comptes-rendus`, corps);
  }

  /** Modifie un compte rendu. */
  modifierCompteRendu(id: string, corps: Record<string, unknown>): Observable<CompteRendu> {
    return this.http.put<CompteRendu>(`${this.base}/api/communication/comptes-rendus/${id}`, corps);
  }

  /** Supprime un compte rendu. */
  supprimerCompteRendu(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/communication/comptes-rendus/${id}`);
  }
}
