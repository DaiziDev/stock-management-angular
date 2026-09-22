/** Type de mouvement de stock — TypeMouvement côté backend. */
export type TypeMouvement = 'ENTREE' | 'SORTIE' | 'AJUSTEMENT';

/** ArticleStockDTO : une ligne de l'état du stock du tenant. */
export interface ArticleStock {
  articleId: number;
  codeArticle: string;
  designation: string;
  stockActuel: number;
  /** Nullable : pas de seuil configuré = pas d'alerte pour cet article. */
  seuilMin: number | null;
}

/** ValorisationResponseDTO : somme (prix HT × quantité) du tenant. */
export interface Valorisation {
  valeurTotale: number;
}

/** MvtStkResponseDTO : une ligne de l'historique des mouvements. */
export interface MvtStk {
  id: number;
  type: TypeMouvement;
  /** Signé pour les AJUSTEMENT (+/-), positif sinon. */
  quantite: number;
  dateMouvement: string;
  /** Motif obligatoire pour un ajustement, null sinon. */
  motif: string | null;
  /** Origine métier (code commande/vente), null pour un ajustement manuel. */
  origine: string | null;
  articleId: number;
  articleDesignation: string;
  /** Niveau de stock de l'article juste après ce mouvement. */
  stockActuelApres: number;
}

/** Charge utile de POST /api/mouvements-stock (ajustement d'inventaire). */
export interface AjustementRequest {
  articleId: number;
  /** Signé : positif pour ajouter, négatif pour retirer. */
  quantite: number;
  /** Obligatoire côté backend (RG-06) : toute correction est tracée. */
  motif: string;
}
