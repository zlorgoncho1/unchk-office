import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  BudgetDetail,
  BudgetResume,
  Communique,
  Courrier,
  NatureCommunique,
  SensCourrier,
  StatutBudget,
  StatutCourrier,
} from './api.models';

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

  // --- Lignes budgétaires (budget réalisé). Le backend renvoie à chaque écriture
  //     le budget complet (entête + lignes + totaux recalculés). On expose donc
  //     consulterBudget() pour charger le détail, puis les opérations sur les lignes. ---

  /**
   * Charge le détail d'un budget (entête + lignes). Les lignes ne sont pas exposées
   * par un endpoint dédié : elles sont portées par BudgetDto (GET /{id}).
   */
  consulterBudget(id: string): Observable<BudgetDetail> {
    return this.http.get<BudgetDetail>(`${this.base}/api/admin/budgets/${id}`);
  }

  /**
   * Liste les lignes d'un budget. S'appuie sur le détail du budget (le backend ne
   * propose pas de route /lignes en lecture seule, les lignes vivent dans le détail).
   */
  listerLignesBudget(id: string): Observable<BudgetDetail['lignes']> {
    return this.consulterBudget(id).pipe(map((b) => b.lignes ?? []));
  }

  /** Ajoute une ligne (corps = CreationLigneBudgetaireDto : category, direction, plannedAmount, label). */
  ajouterLigneBudget(id: string, corps: Record<string, unknown>): Observable<BudgetDetail> {
    return this.http.post<BudgetDetail>(
      `${this.base}/api/admin/budgets/${id}/lignes`,
      corps
    );
  }

  /** Saisit/met à jour le montant réalisé d'une ligne (corps = RealisationLigneDto : realizedAmount). */
  majRealisationLigne(
    id: string,
    ligneId: string,
    corps: Record<string, unknown>
  ): Observable<BudgetDetail> {
    return this.http.patch<BudgetDetail>(
      `${this.base}/api/admin/budgets/${id}/lignes/${ligneId}/realisation`,
      corps
    );
  }

  /** Supprime une ligne budgétaire (renvoie le budget recalculé). */
  supprimerLigneBudget(id: string, ligneId: string): Observable<BudgetDetail> {
    return this.http.delete<BudgetDetail>(
      `${this.base}/api/admin/budgets/${id}/lignes/${ligneId}`
    );
  }

  // --- Exports budgétaires (PDF / Excel). Le binaire est récupéré en Blob
  //     (requête authentifiée par l'intercepteur) puis téléchargé côté navigateur. ---

  /** Exporte l'état budgétaire au format PDF. */
  exporterBudgetPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.base}/api/admin/budgets/${id}/export/pdf`, {
      responseType: 'blob',
    });
  }

  /** Exporte l'état budgétaire au format Excel (xlsx). */
  exporterBudgetExcel(id: string): Observable<Blob> {
    return this.http.get(`${this.base}/api/admin/budgets/${id}/export/excel`, {
      responseType: 'blob',
    });
  }

  // --- Courrier (registre du courrier arrivé / départ) : /api/admin/mails ---

  /** Liste les courriers, filtrés optionnellement par sens et/ou statut. */
  listerCourriers(
    direction?: SensCourrier,
    statut?: StatutCourrier
  ): Observable<Courrier[]> {
    let params = new HttpParams();
    if (direction) {
      params = params.set('direction', direction);
    }
    if (statut) {
      params = params.set('statut', statut);
    }
    return this.http.get<Courrier[]>(`${this.base}/api/admin/mails`, { params });
  }

  /** Enregistre un courrier (corps = CreationMailDto). */
  creerCourrier(corps: Record<string, unknown>): Observable<Courrier> {
    return this.http.post<Courrier>(`${this.base}/api/admin/mails`, corps);
  }

  /** Met à jour un courrier (corps = MajMailDto). */
  modifierCourrier(
    id: string,
    corps: Record<string, unknown>
  ): Observable<Courrier> {
    return this.http.put<Courrier>(`${this.base}/api/admin/mails/${id}`, corps);
  }

  /** Fait évoluer le statut d'un courrier. */
  changerStatutCourrier(
    id: string,
    status: StatutCourrier
  ): Observable<Courrier> {
    return this.http.patch<Courrier>(
      `${this.base}/api/admin/mails/${id}/statut`,
      { status }
    );
  }

  /** Supprime (logiquement) un courrier. */
  supprimerCourrier(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/admin/mails/${id}`);
  }

  // --- Communiqués (notes de service / circulaires) : /api/admin/communiques ---

  /** Liste les communiqués, filtrés optionnellement par nature. */
  listerCommuniques(kind?: NatureCommunique): Observable<Communique[]> {
    let params = new HttpParams();
    if (kind) {
      params = params.set('kind', kind);
    }
    return this.http.get<Communique[]>(`${this.base}/api/admin/communiques`, {
      params,
    });
  }

  /** Crée un communiqué (brouillon). Corps = CreationCommuniqueDto. */
  creerCommunique(corps: Record<string, unknown>): Observable<Communique> {
    return this.http.post<Communique>(
      `${this.base}/api/admin/communiques`,
      corps
    );
  }

  /** Met à jour un communiqué. Corps = MajCommuniqueDto. */
  modifierCommunique(
    id: string,
    corps: Record<string, unknown>
  ): Observable<Communique> {
    return this.http.put<Communique>(
      `${this.base}/api/admin/communiques/${id}`,
      corps
    );
  }

  /** Publie un communiqué → notifications automatiques aux rôles ciblés. */
  publierCommunique(id: string): Observable<Communique> {
    return this.http.patch<Communique>(
      `${this.base}/api/admin/communiques/${id}/publication`,
      {}
    );
  }

  /** Supprime (logiquement) un communiqué. */
  supprimerCommunique(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/admin/communiques/${id}`);
  }
}
