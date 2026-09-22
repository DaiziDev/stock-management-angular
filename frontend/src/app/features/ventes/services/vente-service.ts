import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { type VenteRequest, type VenteResponse } from '../models/vente.model';
import { type Article } from '../../articles/models/article.model';
import { type Client } from '../../clients/models/client.model';

/**
 * Accès API du module ventes / point de vente.
 *
 * Le POS a besoin du catalogue (articles) et du répertoire clients du
 * tenant : ils sont déjà servis par leurs services respectifs -- on les
 * réutilise via injection, sans dupliquer d'appels HTTP ici.
 */
@Injectable({ providedIn: 'root' })
export class VenteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ventes`;

  /** Dernière vente créée (pour le ticket de confirmation du POS). */
  private derniereVenteSignal = signal<VenteResponse | null>(null);
  readonly derniereVente = this.derniereVenteSignal.asReadonly();

  /** Liste des ventes du tenant (la plus récente en premier, côté backend). */
  loadAll(): Observable<VenteResponse[]> {
    return this.http.get<VenteResponse[]>(this.apiUrl);
  }

  /**
   * Encaisse une vente : le backend décrémente le stock immédiatement et
   * refuse en bloc (409) si le stock est insuffisant sur une seule ligne.
   */
  encaisser(dto: VenteRequest): Observable<VenteResponse> {
    return this.http.post<VenteResponse>(this.apiUrl, dto).pipe(
      tap((vente) => this.derniereVenteSignal.set(vente))
    );
  }
}
