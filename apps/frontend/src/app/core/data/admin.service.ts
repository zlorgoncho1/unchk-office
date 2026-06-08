import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { BudgetResume, StatutBudget } from './api.models';

/**
 * Accès aux données « admin » (budgets) via le gateway.
 * Route réelle : /api/admin/budgets (réponse : tableau de résumés).
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste des budgets, éventuellement filtrée par exercice (année). */
  listerBudgets(annee?: number): Observable<BudgetResume[]> {
    let params = new HttpParams();
    if (annee != null) {
      params = params.set('annee', annee);
    }
    return this.http.get<BudgetResume[]>(`${this.base}/api/admin/budgets`, {
      params,
    });
  }

  // --- CRUD budgets (entête). Le backend gère le statut via un endpoint dédié
  //     (PATCH /{id}/statut) ; le statut n'est donc pas porté par le POST/PUT. ---

  /** Crée un projet de budget (corps = CreationBudgetDto : fiscalYear, label, orientationNote, currency). */
  creerBudget(corps: Record<string, unknown>): Observable<BudgetResume> {
    return this.http.post<BudgetResume>(`${this.base}/api/admin/budgets`, corps);
  }

  /** Modifie les attributs d'un budget (corps = MajBudgetDto : label, orientationNote, currency). */
  modifierBudget(id: string, corps: Record<string, unknown>): Observable<BudgetResume> {
    return this.http.put<BudgetResume>(`${this.base}/api/admin/budgets/${id}`, corps);
  }

  /** Fait évoluer le statut d'un budget (corps = ChangementStatutBudgetDto : status). */
  changerStatutBudget(id: string, status: StatutBudget): Observable<BudgetResume> {
    return this.http.patch<BudgetResume>(
      `${this.base}/api/admin/budgets/${id}/statut`,
      { status }
    );
  }

  /** Supprime un budget. */
  supprimerBudget(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/admin/budgets/${id}`);
  }
}
