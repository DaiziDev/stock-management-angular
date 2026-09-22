import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { type EntrepriseCliente, type OnboardingRequest, type PlateformeStats } from '../models/plateforme.models';

/**
 * Payload de PUT /api/entreprises/{id} : coordonnées SANS le nom.
 * (Le backend lève 400 sur un DTO avec nom inconnu / non modifiable.)
 */
export interface EntrepriseUpdatePayload {
  adresse1: string | null;
  adresse2: string | null;
  ville: string | null;
  codePostal: string | null;
  pays: string | null;
  mail: string | null;
  numTel: string | null;
}

/**
 * Accès API du module plateforme (réservé au SUPER_ADMIN côté backend :
 * /api/plateforme et /api/entreprises renvoient 403 pour tout autre rôle).
 */
@Injectable({ providedIn: 'root' })
export class PlateformeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/plateforme`;
  private entreprisesUrl = `${environment.apiUrl}/entreprises`;

  private entreprisesSignal = signal<EntrepriseCliente[]>([]);
  entreprises = this.entreprisesSignal.asReadonly();

  /** Stats globales : compteurs plateformes, toutes entreprises confondues. */
  stats(): Observable<PlateformeStats> {
    return this.http.get<PlateformeStats>(`${this.apiUrl}/stats`);
  }

  loadAll(): void {
    this.http.get<EntrepriseCliente[]>(this.entreprisesUrl).subscribe((data) => this.entreprisesSignal.set(data));
  }

  /** Rechargement silencieux (sans spinner) après une action locale. */
  rafraichir(): void {
    this.loadAll();
  }

  /**
   * Détail d'une entreprise directement depuis l'API : fiable même si la
   * liste n'est pas (encore) en mémoire -- contrairement à un findById
   * sur un signal alimenté par la page précédente.
   */
  findById(id: number): Observable<EntrepriseCliente> {
    return this.http.get<EntrepriseCliente>(`${this.entreprisesUrl}/${id}`);
  }

  /**
   * Mise à jour des coordonnées d'une entreprise cliente. Le nom n'est PAS
   * envoyé (identifiant de cloisonnement, non modifiable côté backend).
   */
  update(id: number, payload: EntrepriseUpdatePayload): Observable<EntrepriseCliente> {
    return this.http.put<EntrepriseCliente>(`${this.entreprisesUrl}/${id}`, payload);
  }

  /**
   * Onboarding : crée l'entreprise cliente ET son premier compte ADMIN en
   * une seule transaction côté backend (soit tout est enregistré, soit rien).
   */
  onboarder(dto: OnboardingRequest): Observable<EntrepriseCliente> {
    return this.http.post<EntrepriseCliente>(`${this.apiUrl}/entreprises`, dto);
  }

  /**
   * Extrait le message lisible du format ApiError du backend
   * (ex. "Une entreprise avec le nom 'X' existe déjà").
   */
  erreurLisible(error: unknown, fallback: string): string {
    const err = error as { error?: { message?: string }; message?: string };
    return err?.error?.message || err?.message || fallback;
  }
}
