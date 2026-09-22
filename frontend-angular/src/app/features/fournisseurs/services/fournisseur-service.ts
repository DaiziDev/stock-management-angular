import { HttpClient } from "@angular/common/http";
import { Observable, finalize, tap } from "rxjs";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Fournisseur, FournisseurRequest } from "../models/fournisseur.model";

@Injectable({ providedIn: 'root' })
export class FournisseurService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/fournisseurs`;

    private fournisseursSignal = signal<Fournisseur[]>([]);
    fournisseurs = this.fournisseursSignal.asReadonly();

    /** Vrai pendant la requête de liste -- consommé par les écrans. */
    readonly chargement = signal(false);

    loadAll(): Observable<Fournisseur[]> {
        this.chargement.set(true);
        return this.http.get<Fournisseur[]>(this.apiUrl).pipe(
            tap((data) => this.fournisseursSignal.set(data)),
            finalize(() => this.chargement.set(false))
        );
    }

    create(dto: FournisseurRequest) {
        return this.http.post<Fournisseur>(this.apiUrl, dto);
    }

    update(id: number, dto: FournisseurRequest) {
        return this.http.put<Fournisseur>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}
