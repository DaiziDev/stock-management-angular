import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { type ArticleStock, type Valorisation, type MvtStk, type AjustementRequest, type TypeMouvement } from '../models/rapports.model';

/**
 * Accès API du module rapports & stock. Lecture surtout (état, alertes,
 * valorisation, historique) + le seul POST autorisé : l'ajustement
 * d'inventaire avec motif obligatoire (RG-06). Les mouvements ENTREE/SORTIE
 * ne sont jamais créés ici : ils proviennent des ventes et commandes.
 */
@Injectable({ providedIn: 'root' })
export class RapportsService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/stock`;
  private mvtUrl = `${environment.apiUrl}/mouvements-stock`;

  etat(): Observable<ArticleStock[]> {
    return this.http.get<ArticleStock[]>(`${this.apiUrl}/etat`);
  }

  alertes(): Observable<ArticleStock[]> {
    return this.http.get<ArticleStock[]>(`${this.apiUrl}/alertes`);
  }

  valorisation(): Observable<Valorisation> {
    return this.http.get<Valorisation>(`${this.apiUrl}/valorisation`);
  }

  /** Historique filtrable par article et/ou type (ENTREE/SORTIE/AJUSTEMENT). */
  mouvements(articleId?: number, type?: TypeMouvement): Observable<MvtStk[]> {
    let params = new HttpParams();
    if (articleId != null) params = params.set('articleId', articleId);
    if (type) params = params.set('type', type);
    return this.http.get<MvtStk[]>(this.mvtUrl, { params });
  }

  /**
   * Ajustement manuel (correction d'inventaire) : quantité SIGNÉE (+/-),
   * motif obligatoire. Le backend refuse une sortie au-delà du stock.
   */
  ajuster(dto: AjustementRequest): Observable<MvtStk> {
    return this.http.post<MvtStk>(this.mvtUrl, dto);
  }
}
