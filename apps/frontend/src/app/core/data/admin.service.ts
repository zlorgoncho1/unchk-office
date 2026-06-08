import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { BudgetResume } from './api.models';

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
}
