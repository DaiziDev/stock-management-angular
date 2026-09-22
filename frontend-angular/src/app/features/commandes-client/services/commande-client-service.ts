import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { type CommandeClient, type CommandeClientRequest } from '../models/commande-client.model';

/**
 * Accès API du module commandes client. Aucun état partagé en signal ici :
 * les deux écrans (liste + création) rechargent leur donnée — plus fiable
 * qu'un cache qui peut être périmé quand une commande est validée ailleurs.
 */
@Injectable({ providedIn: 'root' })
export class CommandeClientService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/commandes-client`;

  loadAll(): Observable<CommandeClient[]> {
    return this.http.get<CommandeClient[]>(this.apiUrl);
  }

  /** Utilisé par l'écran de création pour recharger après erreur de validation. */
  findById(id: number): Observable<CommandeClient> {
    return this.http.get<CommandeClient>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crée la commande + ses lignes en une requête ; le backend fixe le code
   * (CC-000042), le statut EN_COURS et envoie l'email de confirmation.
   */
  creer(dto: CommandeClientRequest): Observable<CommandeClient> {
    return this.http.post<CommandeClient>(this.apiUrl, dto);
  }

  /**
   * EN_COURS -> VALIDEE : le backend génère les sorties de stock et refuse
   * en bloc (409) si une seule ligne dépasse le stock disponible.
   */
  valider(id: number): Observable<CommandeClient> {
    return this.http.put<CommandeClient>(`${this.apiUrl}/${id}/valider`, {});
  }

  /** VALIDEE -> EXPEDIEE : marchandise remise au transporteur, aucun impact stock. */
  expedier(id: number): Observable<CommandeClient> {
    return this.http.put<CommandeClient>(`${this.apiUrl}/${id}/expedier`, {});
  }

  /** EXPEDIEE -> LIVREE (raccourci toléré depuis VALIDEE) : aucun impact stock. */
  livrer(id: number): Observable<CommandeClient> {
    return this.http.put<CommandeClient>(`${this.apiUrl}/${id}/livrer`, {});
  }

  /** EN_COURS -> ANNULEE, sans impact stock. */
  annuler(id: number): Observable<CommandeClient> {
    return this.http.put<CommandeClient>(`${this.apiUrl}/${id}/annuler`, {});
  }
}
