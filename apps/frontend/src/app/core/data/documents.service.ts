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
}
