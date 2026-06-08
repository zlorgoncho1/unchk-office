import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Document, PageReponse } from './api.models';

/**
 * Accès à la gestion documentaire via le gateway.
 * Route réelle : /api/documents (réponse paginée).
 */
@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Liste paginée des documents, filtrable par catégorie. */
  lister(page = 0, size = 20, category?: string): Observable<PageReponse<Document>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<PageReponse<Document>>(`${this.base}/api/documents`, {
      params,
    });
  }

  // --- CRUD documents ---
  // NB : la création est multipart (métadonnées JSON « metadata » + fichier binaire « file »
  //      vers MinIO). La mise à jour des métadonnées est un PATCH (titre, description,
  //      archived, visibility). La suppression est logique côté backend.

  /**
   * Dépose un nouveau document (multipart : métadonnées + fichier binaire).
   * Le champ « metadata » porte le DTO CreerDocumentRequete (title, category, description,
   * visibility, sourceService, sourceRef) ; le champ « file » porte le binaire.
   */
  creer(metadata: Record<string, unknown>, fichier: File): Observable<Document> {
    const corps = new FormData();
    // Les métadonnées voyagent en JSON sous le nom de part « metadata ».
    corps.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );
    corps.append('file', fichier);
    return this.http.post<Document>(`${this.base}/api/documents`, corps);
  }

  /**
   * Dépose un document à partir d'un formulaire typé (titre, catégorie, description,
   * visibilité) + le fichier binaire. Construit la requête multipart attendue par le
   * backend : la part « metadata » porte le JSON, la part « file » porte le binaire.
   * On ne fixe PAS le Content-Type : le navigateur ajoute lui-même la boundary multipart.
   */
  creerDocument(
    meta: {
      title: string;
      category: string;
      description?: string;
      visibility?: string[];
    },
    fichier: File
  ): Observable<Document> {
    const corps = new FormData();
    // Métadonnées en JSON sous le nom de part « metadata » (Blob application/json).
    corps.append(
      'metadata',
      new Blob([JSON.stringify(meta)], { type: 'application/json' })
    );
    // Fichier binaire sous le nom de part « file » (on conserve le nom d'origine).
    corps.append('file', fichier, fichier.name);
    return this.http.post<Document>(`${this.base}/api/documents`, corps);
  }

  /**
   * Met à jour les métadonnées d'un document (PATCH côté backend).
   * Corps = MettreAJourDocumentRequete : title, description, archived, visibility.
   */
  modifier(id: string, corps: Record<string, unknown>): Observable<Document> {
    return this.http.patch<Document>(`${this.base}/api/documents/${id}`, corps);
  }

  /** Supprime (logiquement) un document. */
  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/documents/${id}`);
  }
}
