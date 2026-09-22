/**
 * Cycle de vie côté backend : StatutCommandeClient (§3.4).
 * EN_COURS -> VALIDEE (sorties de stock) -> EXPEDIEE -> LIVREE, ou ANNULEE.
 */
export type StatutCommandeClient = 'EN_COURS' | 'VALIDEE' | 'EXPEDIEE' | 'LIVREE' | 'ANNULEE';

/** Ligne d'une commande — LigneCommandeResponseDTO côté backend. */
export interface LigneCommande {
  id: number;
  articleId: number;
  articleDesignation: string;
  quantite: number;
  prixUnitaire: number;
  sousTotal: number;
}

/** CommandeClientResponseDTO côté backend (dateCommande en ISO). */
export interface CommandeClient {
  id: number;
  code: string;
  dateCommande: string;
  statut: StatutCommandeClient;
  clientId: number;
  clientNom: string;
  lignes: LigneCommande[];
  total: number;
}

/** Charge utile de POST /api/commandes-client. */
export interface CommandeClientRequest {
  clientId: number;
  lignes: { articleId: number; quantite: number }[];
}
