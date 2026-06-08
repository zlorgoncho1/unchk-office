import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Etudiant, PageReponse, Personnel } from './api.models';

/**
 * Accès aux données « people » (étudiants + personnel) via le gateway.
 * Routes réelles : /api/people/students, /api/people/staff, /api/etudiants/me.
 */
@Injectable({ providedIn: 'root' })
export class PeopleService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Fiche de l'étudiant connecté (résolue côté serveur via le claim sub). */
  maFiche(): Observable<Etudiant> {
    return this.http.get<Etudiant>(`${this.base}/api/etudiants/me`);
  }

  /** Liste paginée des étudiants (réservée au personnel par le RBAC du gateway). */
  listerEtudiants(page = 0, size = 20): Observable<PageReponse<Etudiant>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageReponse<Etudiant>>(
      `${this.base}/api/people/students`,
      { params }
    );
  }

  /** Liste paginée du personnel actif. */
  listerPersonnel(page = 0, size = 20): Observable<PageReponse<Personnel>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageReponse<Personnel>>(
      `${this.base}/api/people/staff`,
      { params }
    );
  }
}
