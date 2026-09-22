import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import type { CurrentUser, LoginResponse, UserRole } from '../models/models';

/** Forme de la réponse GET /api/auth/me (CurrentUserResponse côté backend). */
interface MeResponse {
  id: number;
  nom: string;
  prenom: string;
  login: string;
  role: UserRole;
  entrepriseId: number | null;
  entrepriseNom: string | null;
}

const TOKEN_KEY = 'sgs.token';
const REFRESH_KEY = 'sgs.refreshToken';
const USER_KEY = 'sgs.currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  /** Utilisateur courant (signal) — toute l'app (sidebar, guards) réagit à ce signal. */
  readonly user = signal<CurrentUser | null>(this.restoreUser());

  /**
   * Authentifie l'utilisateur via POST /api/auth/login.
   * En cas de succès : stocke le JWT + le profil, et met à jour le signal `user`.
   * En cas d'échec (401/403) : l'appelant reçoit l'erreur (voir Login.submit()).
   */
  login(login: string, motDePasse: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { login, motDePasse }).pipe(
      tap((response) => this.memoriserSession(response))
    );
  }

  /**
   * Échange le refresh token contre un nouveau couple JWT + refresh
   * (rotation côté backend : l'ancien refresh devient inutilisable).
   * Appelé par l'errorInterceptor quand le backend répond 401 avec le
   * flag `tokenExpiré` — permet de prolonger la session sans repasser
   * par /login tant que le refresh token (7 jours) est valide.
   */
  refreshSession(): Observable<LoginResponse | null> {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    // Pas de refresh token stocké : rien à tenter (visiteur anonyme ou
    // session déjà consommée) — on refuse sans appel réseau.
    if (!refreshToken) {
      return of(null);
    }
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/refresh`, { refreshToken })
      .pipe(
        tap((response) => this.memoriserSession(response)),
        catchError(() => of(null)),
      );
  }

  /**
   * Déconnexion : révoque le refresh token côté serveur (best-effort :
   * même si l'appel échoue, on purge le localStorage pour forcer la
   * reconnexion locale), puis nettoie l'état local.
   */
  logout(): void {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      // Fire-and-forget : la déconnexion locale ne doit pas dépendre du réseau.
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe({ error: () => void 0 });
    }
    this.user.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /**
   * Resynchronise le profil local avec le backend (GET /api/auth/me).
   *
   * Indispensable car le localStorage peut contenir un profil PÉRIMÉ : par
   * exemple le compte bootstrap promu ADMIN d'entreprise → SUPER_ADMIN par
   * le DataInitializer. Sans resynchro, l'app croit encore au vieux rôle et
   * envoie le superadmin sur l'espace entreprise au lieu de la console
   * plateforme.
   *
   * Échoue silencieusement (token expiré, backend down...) : on garde le
   * profil local et l'errorInterceptor déconnectera au prochain 401.
   */
  syncSession(): Observable<CurrentUser | null> {
    // Pas de token = pas de session à synchroniser (évite un 401 systématique
    // pour tout visiteur anonyme au chargement de l'app).
    if (!this.getToken()) {
      return of(null);
    }
    return this.http.get<MeResponse>(`${this.apiUrl}/me`).pipe(
      tap({
        next: (me) => {
          const user: CurrentUser = {
            id: me.id,
            nom: `${me.prenom} ${me.nom}`.trim(),
            role: me.role,
            entrepriseId: me.entrepriseId,
            entrepriseName: me.entrepriseNom,
            login: me.login,
          };
          this.user.set(user);
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        },
        error: () => {
          /* profil local conservé ; un 401 déclenchera la déconnexion via errorInterceptor */
        },
      }),
      // La valeur retournée intéresse peu les appelants : on expose surtout
      // l'effet de bord (signal + localStorage mis à jour).
      map(() => this.user()),
      catchError(() => of(null)),
    );
  }

  /**
   * Purge locale seule (sans appel serveur) : utilisée quand la session est
   * définitivement morte (refresh token absent/invalide) — appeler le
   * serveur serait inutile, voire créerait une boucle d'erreurs.
   */
  logoutLocal(): void {
    this.user.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /** Lu par l'intercepteur HTTP pour poser le header Authorization. */
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  /** Lu par l'errorInterceptor pour tenter un renouvellement avant déconnexion. */
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  /** ⭐ Vérifie si l'utilisateur courant possède l'un des rôles donnés. */
  hasRole(...roles: UserRole[]): boolean {
    const role = this.user()?.role;
    return !!role && roles.includes(role);
  }

  /**
   * Vrai pour l'opérateur de la plateforme (SUPER_ADMIN) : il n'appartient
   * à aucune entreprise cliente et est dirigé vers la console plateforme
   * (guards + redirection post-login).
   */
  isPlatformAdmin(): boolean {
    return this.user()?.role === 'SUPER_ADMIN';
  }

  private restoreUser(): CurrentUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as CurrentUser) : null;
    } catch {
      return null;
    }
  }

  /** Stocke le couple de tokens + le profil, et met à jour le signal user. */
  private memoriserSession(response: LoginResponse): void {
    const user: CurrentUser = {
      id: response.user.id,
      nom: `${response.user.prenom} ${response.user.nom}`.trim(),
      role: response.user.role,
      entrepriseId: response.user.entrepriseId,
      entrepriseName: response.user.entrepriseNom,
      login: response.user.login,
    };
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(REFRESH_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.user.set(user);
  }
}
