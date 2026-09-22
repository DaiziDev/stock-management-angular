import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../../core/services/services';
import type {
  Utilisateur,
  UtilisateurCreateRequest,
  UtilisateurUpdateRequest,
} from '../models/utilisateur.model';

/**
 * Accès API pour la gestion des comptes de l'entreprise (ADMIN uniquement).
 *
 * Particularité : la CRÉATION ne passe pas par /api/utilisateurs mais par
 * POST /api/auth/register -- c'est le seul endpoint de création exposé par
 * le backend, et il applique déjà les règles métier (ADMIN limité à SA
 * propre entreprise, mot de passe hashé côté serveur).
 *
 * La liste est gardée dans un signal (même pattern que ClientService) pour
 * éviter les rechargements entre la liste et le formulaire d'édition, qui
 * re-souscrit simplement à loadAll().
 */
@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private apiUrl = `${environment.apiUrl}/utilisateurs`;
  private registerUrl = `${environment.apiUrl}/auth/register`;

  private utilisateursSignal = signal<Utilisateur[]>([]);
  readonly utilisateurs = this.utilisateursSignal.asReadonly();

  /** Vrai pendant la requête de liste -- consommé par les écrans. */
  readonly chargement = signal(false);

  loadAll(): Observable<Utilisateur[]> {
    this.chargement.set(true);
    return this.http.get<Utilisateur[]>(this.apiUrl).pipe(
      tap((data) => this.utilisateursSignal.set(data)),
      finalize(() => this.chargement.set(false))
    );
  }

  /** Création : endpoint dédié /api/auth/register (réservé ADMIN). */
  create(dto: UtilisateurCreateRequest): Observable<unknown> {
    return this.http.post(this.registerUrl, dto);
  }

  update(id: number, dto: UtilisateurUpdateRequest): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * L'entreprise de l'ADMIN connecté -- obligatoire pour construire le
   * RegisterRequest (le backend refuse un entrepriseId différent du sien).
   */
  entrepriseCouranteId(): number | null {
    return this.auth.user()?.entrepriseId ?? null;
  }
}
