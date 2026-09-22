export interface CategorieSummary {
    id: number;
    designation: string;
}

export interface Article {
    id: number;
    codeArticle: string;
    designation : string;
    prixUnitaireHt: number;
    tauxTva: number;
    prixUnitaireTtc: number;
    photo : string | null;
    categorie: CategorieSummary;

    /** Niveau de stock temps réel (décrémenté à chaque vente côté backend). */
    stockActuel?: number;
}

export interface ArticleRequest {
    codeArticle: string;
    designation: string;
    prixUnitaireHt: number;
    tauxTva: number;
    photo: string | null;
    categorieId: number;
}