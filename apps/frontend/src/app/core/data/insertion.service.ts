import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ContactRegistre,
  Partenaire,
  SituationInsertionDto,
  Stage,
  StatistiquesInsertion,
} from './api.models';

/**
 * Accès aux données « insertion » (stages, partenaires, statistiques) via le gateway.
 * Routes réelles : /api/insertion/stages, /api/insertion/partenaires, /api/insertion/statistiques.
 */
@Injectable({ providedIn: 'root' })
export class InsertionService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste des stages, éventuellement filtrée par étudiant. */
  listerStages(etudiant?: string): Observable<Stage[]> {
    let params = new HttpParams();
    if (etudiant) {
      params = params.set('etudiant', etudiant);
    }
    return this.http.get<Stage[]>(`${this.base}/api/insertion/stages`, {
      params,
    });
  }

  /** Liste des partenaires actifs. */
  listerPartenaires(): Observable<Partenaire[]> {
    return this.http.get<Partenaire[]>(
      `${this.base}/api/insertion/partenaires`
    );
  }

  /** Statistiques d'insertion agrégées (par type et par formation). */
  statistiques(): Observable<StatistiquesInsertion> {
    return this.http.get<StatistiquesInsertion>(
      `${this.base}/api/insertion/statistiques`
    );
  }

  /** Export PDF des statistiques d'insertion (fichier binaire, JWT ajouté par l'intercepteur). */
  exporterStatistiquesPdf(): Observable<Blob> {
    return this.http.get(`${this.base}/api/insertion/statistiques/export/pdf`, {
      responseType: 'blob',
    });
  }

  /** Export Excel des statistiques d'insertion (fichier binaire, JWT ajouté par l'intercepteur). */
  exporterStatistiquesExcel(): Observable<Blob> {
    return this.http.get(`${this.base}/api/insertion/statistiques/export/excel`, {
      responseType: 'blob',
    });
  }

  // --- CRUD partenaires (corps = PartnerRequest : name, kind, sector, contactName,
  //     contactEmail, contactPhone, address, city) ---

  /** Crée un partenaire. */
  creerPartenaire(corps: Record<string, unknown>): Observable<Partenaire> {
    return this.http.post<Partenaire>(`${this.base}/api/insertion/partenaires`, corps);
  }

  /** Modifie un partenaire existant. */
  modifierPartenaire(id: string, corps: Record<string, unknown>): Observable<Partenaire> {
    return this.http.put<Partenaire>(`${this.base}/api/insertion/partenaires/${id}`, corps);
  }

  /** Supprime un partenaire. */
  supprimerPartenaire(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/insertion/partenaires/${id}`);
  }

  // --- CRUD stages (corps = StageRequest) ---

  /** Crée un stage / bilan. */
  creerStage(corps: Record<string, unknown>): Observable<Stage> {
    return this.http.post<Stage>(`${this.base}/api/insertion/stages`, corps);
  }

  /** Modifie un stage. */
  modifierStage(id: string, corps: Record<string, unknown>): Observable<Stage> {
    return this.http.put<Stage>(`${this.base}/api/insertion/stages/${id}`, corps);
  }

  /** Supprime un stage. */
  supprimerStage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/insertion/stages/${id}`);
  }

  // --- Registre de contact (/api/insertion/contacts) — suivi du devenir des diplômés ---

  /** Historique de contact d'un étudiant (registre). */
  historiqueContacts(studentRef: string): Observable<ContactRegistre[]> {
    return this.http.get<ContactRegistre[]>(
      `${this.base}/api/insertion/contacts/etudiant/${studentRef}`
    );
  }

  /** Enregistre un contact de suivi (corps = ContactLogRequest). */
  enregistrerContact(corps: Record<string, unknown>): Observable<ContactRegistre> {
    return this.http.post<ContactRegistre>(`${this.base}/api/insertion/contacts`, corps);
  }

  // --- Situations d'insertion (/api/insertion/situations) — saisie alimentant les stats ---

  /** Situations d'insertion déclarées pour un étudiant. */
  situationsEtudiant(studentRef: string): Observable<SituationInsertionDto[]> {
    return this.http.get<SituationInsertionDto[]>(
      `${this.base}/api/insertion/situations/etudiant/${studentRef}`
    );
  }

  /** Déclare une situation d'insertion (corps = InsertionOutcomeRequest). */
  declarerSituation(corps: Record<string, unknown>): Observable<SituationInsertionDto> {
    return this.http.post<SituationInsertionDto>(`${this.base}/api/insertion/situations`, corps);
  }

  /** Modifie une situation d'insertion existante. */
  modifierSituation(
    id: string,
    corps: Record<string, unknown>
  ): Observable<SituationInsertionDto> {
    return this.http.put<SituationInsertionDto>(
      `${this.base}/api/insertion/situations/${id}`,
      corps
    );
  }
}
