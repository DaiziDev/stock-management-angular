import { HttpClient } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Entreprise, EntrepriseRequest } from "../models/entreprise.model";

@Injectable({ providedIn: 'root' })
export class EntrepriseService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/entreprises`;

    private entreprisesSignal = signal<Entreprise[]>([]);
    entreprises = this.entreprisesSignal.asReadonly();

    loadAll() {
        this.http.get<Entreprise[]>(this.apiUrl)
        .subscribe(data => this.entreprisesSignal.set(data));
    }

    create(dto: EntrepriseRequest) {
        return this.http.post<Entreprise>(this.apiUrl, dto);
    }

    update(id: number, dto: EntrepriseRequest) {
        return this.http.put<Entreprise>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}
