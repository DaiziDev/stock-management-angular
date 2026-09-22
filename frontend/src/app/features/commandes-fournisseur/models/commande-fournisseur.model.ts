/**
 * Cycle de vie côté backend : StatutCommandeFournisseur (§3.5).
 * EN_ATTENTE -> RECUE_PARTIELLEMENT -> RECUE, ou ANNULEE (depuis EN_ATTENTE).
 */
export type StatutCommandeFournisseur = 'EN_ATTENTE' | 'RECUE_PARTIELLEMENT' | 'RECUE' | 'ANNULEE';

/** Ligne d'une commande d'achat — prix en HT (on achète hors taxes). */
export interface LigneCommandeFournisseur {
  id: number;
  articleId: number;
  articleDesignation: string;
  quantite: number;
  /** Quantité déjà réceptionnée (réception partielle, §3.5). Toujours <= quantite. */
  quantiteRecue: number;
  prixUnitaire: number;
  sousTotal: number;
}

/** CommandeFournisseurResponseDTO côté backend (dateCommande en ISO). */
export interface CommandeFournisseur {
  id: number;
  code: string;
  dateCommande: string;
  statut: StatutCommandeFournisseur;
  fournisseurId: number;
  fournisseurNom: string;
  lignes: LigneCommandeFournisseur[];
  total: number;
}

/** Charge utile de POST /api/commandes-fournisseur. */
export interface CommandeFournisseurRequest {
  fournisseurId: number;
  lignes: { articleId: number; quantite: number }[];
}

/** Charge utile de PUT /api/commandes-fournisseur/{id}/receptionner-partiel. */
export interface ReceptionPartielleRequest {
  lignes: { ligneId: number; quantiteRecue: number }[];
}
