import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  type ArticleStockDTO,
  type DashboardKpis,
  type EntreeSortieJour,
  type GraphiquesResponse,
  type TopArticle,
  type VenteListe,
} from '../models/dashboard.models';

/**
 * Accès API du tableau de bord d'entreprise. Combine trois sources réelles :
 * - GET /api/dashboard/kpis   : KPIs agrégés côté backend (tenant-scopé) ;
 * - GET /api/stock/alertes    : articles sous leur seuil ;
 * - GET /api/ventes           : ventes (pour la série CA des 7 jours).
 *
 * Un endpoint en erreur (ex: 403 pour un VENDEUR sur /kpis) ne bloque pas
 * les autres : la source tombe sur une valeur par défaut.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dashboard`;
  private stockUrl = `${environment.apiUrl}/stock`;
  private ventesUrl = `${environment.apiUrl}/ventes`;

  /** Charge les 3 sources en parallèle, tolérante aux échecs partiels. */
  chargerTout() {
    return forkJoin({
      kpis: this.http
        .get<DashboardKpis>(`${this.apiUrl}/kpis`)
        .pipe(catchError(() => of<DashboardKpis | null>(null))),
      alertes: this.http
        .get<ArticleStockDTO[]>(`${this.stockUrl}/alertes`)
        .pipe(catchError(() => of<ArticleStockDTO[]>([]))),
      ventes: this.http
        .get<VenteListe[]>(`${this.ventesUrl}`)
        .pipe(catchError(() => of<VenteListe[]>([]))),
    });
  }

  /**
   * Données des graphiques (évolution entrées/sorties + top articles).
   * Tolérant aux erreurs : en cas d'échec, les deux séries tombent sur des
   * tableaux vides et le dashboard affiche un état vide propre.
   */
  chargerGraphiques() {
    return this.http.get<GraphiquesResponse>(`${this.apiUrl}/graphiques`).pipe(
      catchError(() =>
        of<GraphiquesResponse>({ evolutionStock: [], topArticles: [] })
      )
    );
  }

  /** Transforme la série backend en labels courts ("1 sept.", "2 oct."...). */
  labelsJours(evolution: EntreeSortieJour[]): string[] {
    return evolution.map((j) => {
      const d = new Date(j.date + 'T00:00:00'); // ISO date-only → locale
      return Number.isNaN(d.getTime())
        ? j.date
        : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    });
  }

  /**
   * CA par jour sur les 7 derniers jours (index 0 = J-6 ... 6 = aujourd'hui),
   * à partir des ventes fournies (total TTC par vente, dateVente ISO).
   */
  calculerSerieCA(ventes: VenteListe[]): number[] {
    const serie = Array(7).fill(0) as number[];
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);

    for (const v of ventes) {
      const d = new Date(v.dateVente);
      if (Number.isNaN(d.getTime())) continue;
      const ecartJours = Math.floor((aujourdhui.getTime() - d.setHours(0, 0, 0, 0)) / 86_400_000);
      if (ecartJours >= 0 && ecartJours < 7) {
        serie[6 - ecartJours] += Number(v.total ?? 0);
      }
    }
    return serie;
  }
}
