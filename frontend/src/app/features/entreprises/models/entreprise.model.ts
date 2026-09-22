export interface Entreprise {
    id: number;
    nom: string;
    adresse1: string | null;
    adresse2: string | null;
    ville: string | null;
    codePostal: string | null;
    pays: string | null;
    mail: string | null;
    numTel: string | null;
}

export interface EntrepriseRequest {
    nom: string;
    adresse1: string | null;
    adresse2: string | null;
    ville: string | null;
    codePostal: string | null;
    pays: string | null;
    mail: string | null;
    numTel: string | null;
}
