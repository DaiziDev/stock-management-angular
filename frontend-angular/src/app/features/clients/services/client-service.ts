import { HttpClient } from "@angular/common/http";
import { Observable, finalize, tap } from "rxjs";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Client, ClientRequest } from "../models/client.model";

@Injectable({ providedIn: 'root' })
export class ClientService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/clients`;

    private clientsSignal = signal<Client[]>([]);
    clients = this.clientsSignal.asReadonly();

    /** Vrai pendant la requête de liste -- consommé par les écrans. */
    readonly chargement = signal(false);

    loadAll(): Observable<Client[]> {
        this.chargement.set(true);
        return this.http.get<Client[]>(this.apiUrl).pipe(
            tap((data) => this.clientsSignal.set(data)),
            finalize(() => this.chargement.set(false))
        );
    }

    create(dto: ClientRequest) {
        return this.http.post<Client>(this.apiUrl, dto);
    }

    update(id: number, dto: ClientRequest) {
        return this.http.put<Client>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}
