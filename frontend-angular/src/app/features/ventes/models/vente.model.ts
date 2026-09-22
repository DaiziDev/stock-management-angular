/** Charge utile de POST /api/ventes (VenteRequestDTO côté backend). */
export interface LigneVenteRequest {
  articleId: number;
  quantite: number;
}

/** Charge utile de POST /api/ventes : vente + lignes en une seule requête. */
export interface VenteRequest {
  /** Nullable : vente au comptoir sans client identifié. */
  clientId: number | null;
  lignes: LigneVenteRequest[];
}

/** Réponse de l'API ventes (VenteResponseDTO côté backend). */
export interface VenteResponse {
  id: number;
  code: string;
  dateVente: string;
  clientId: number | null;
  clientNom: string;
  lignes: {
    id: number;
    articleId: number;
    articleDesignation: string;
    quantite: number;
    prixUnitaire: string;
    sousTotal: string;
  }[];
  total: string;
}
