/** KPIs renvoyés par GET /api/dashboard/kpis (BigDecimal → string en JSON). */
export interface DashboardKpis {
  valeurStock: string;
  nbArticlesEnAlerte: number;
  nbCommandesClientEnCours: number;
  nbCommandesFournisseurEnAttente: number;
  nbVentesDuMois: number;
  chiffreAffairesDuMois: string;
}

/** Article en alerte de stock (GET /api/stock/alertes). */
export interface ArticleStockDTO {
  id: number;
  codeArticle: string;
  designation: string;
  stockActuel: number;
  seuilMin: number | null;
}

/** Vente liste (GET /api/ventes) -- sous-ensemble utilisé par le dashboard. */
export interface VenteListe {
  id: number;
  code: string;
  dateVente: string;
  clientNom: string | null;
  total: string;
}

/** Un jour de la série entrées / sorties de stock (GET /api/dashboard/graphiques). */
export interface EntreeSortieJour {
  date: string; // ISO : "2026-09-19"
  entrees: number;
  sorties: number;
}

/** Un article du classement des meilleures ventes (GET /api/dashboard/graphiques). */
export interface TopArticle {
  articleId: number;
  designation: string;
  codeArticle: string;
  quantiteVendue: number;
  /** BigDecimal → string en JSON. */
  chiffreAffaires: string;
}

/** Réponse complète de GET /api/dashboard/graphiques. */
export interface GraphiquesResponse {
  evolutionStock: EntreeSortieJour[];
  topArticles: TopArticle[];
}
