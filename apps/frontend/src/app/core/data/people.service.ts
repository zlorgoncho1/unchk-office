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

  // --- CRUD étudiants (corps = CreerEtudiantRequest / ModifierEtudiantRequest :
  //     ine (création seulement), firstName, lastName, gender, email, phone,
  //     promotion, enrollmentYear, exitYear, status, matricule, birthDate...) ---

  /** Crée un étudiant. */
  creerEtudiant(corps: Record<string, unknown>): Observable<Etudiant> {
    return this.http.post<Etudiant>(`${this.base}/api/people/students`, corps);
  }

  /** Modifie un étudiant existant. */
  modifierEtudiant(id: string, corps: Record<string, unknown>): Observable<Etudiant> {
    return this.http.put<Etudiant>(`${this.base}/api/people/students/${id}`, corps);
  }

  /** Supprime (logiquement) un étudiant. */
  supprimerEtudiant(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/people/students/${id}`);
  }

  // --- CRUD personnel (corps = CreerPersonnelRequest / ModifierPersonnelRequest :
  //     firstName, lastName, gender, kind, grade, speciality, department,
  //     email, phone, matricule, active...) ---

  /** Crée un membre du personnel. */
  creerPersonnel(corps: Record<string, unknown>): Observable<Personnel> {
    return this.http.post<Personnel>(`${this.base}/api/people/staff`, corps);
  }

  /** Modifie un membre du personnel existant. */
  modifierPersonnel(id: string, corps: Record<string, unknown>): Observable<Personnel> {
    return this.http.put<Personnel>(`${this.base}/api/people/staff/${id}`, corps);
  }

  /** Supprime (logiquement) un membre du personnel. */
  supprimerPersonnel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/people/staff/${id}`);
  }
}
