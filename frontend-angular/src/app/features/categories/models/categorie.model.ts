export interface Categorie  {
    id: number; 
    code: string;
    designation: string;
}

export interface CategorieRequest {
    code : string;
    designation: string;
}