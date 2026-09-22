export interface Client {
    id: number;
    nom: string;
    prenom: string;
    adresse1: string | null;
    adresse2: string | null;
    ville: string | null;
    codePostal: string | null;
    pays: string | null;
    mail: string | null;
    numTel: string | null;
    photo: string | null;
}

export interface ClientRequest {
    nom: string;
    prenom: string;
    adresse1: string | null;
    adresse2: string | null;
    ville: string | null;
    codePostal: string | null;
    pays: string | null;
    mail: string | null;
    numTel: string | null;
    photo: string | null;
}
