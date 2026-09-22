import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, type Observable, catchError, filter, finalize, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/services';
import type { LoginResponse } from '../models/models';

/**
 * 401 → tentative de refresh → rejeu de la requête, sinon déconnexion.
 *
 * Depuis l'ajout du refresh token (§6.3) : un 401 ne signifie plus
 * "session morte" mais souvent juste "JWT de 24h expiré". On tente alors
 * POST /api/auth/refresh (rotation côté backend) et on rejoue la requête
 * originelle avec le nouveau token — l'utilisateur ne voit rien.
 *
 * Garanties :
 * - UN SEUL appel /refresh à la fois (single-flight) : 10 requêtes qui
 *   échouent en même temps partagent le même renouvellement, au lieu de
 *   consommer 10 refresh tokens (dont 9 rejetés par la rotation) ;
 * - chaque requête n'est rejouée qu'UNE fois (header marqueur X-Auth-Retry) :
 *   si le rejeu 401 encore, on déconnecte au lieu de boucler ;
 * - les endpoints d'auth eux-mêmes (login/refresh/logout) ne déclenchent
 *   pas de refresh — un 401 sur /login est un mauvais mot de passe, il doit
 *   remonter au formulaire.
 */

/**
 * État du renouvellement partagé entre requêtes concurrentes (variable de
 * module = un seul état pour toute l'app). undefined = "en cours, patienter",
 * null = échec, LoginResponse = succès.
 */
const refreshEnCours$ = new BehaviorSubject<LoginResponse | null | undefined>(undefined);
let isRefreshing = false;

/** Endpoints exclus du mécanisme refresh + rejeu. */
function estEndpointAuth(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      const peutRafraichir =
        error?.status === 401 && !estEndpointAuth(req.url) && !req.headers.has('X-Auth-Retry');

      if (!peutRafraichir) {
        // 401 sans espoir de refresh (rejeu déjà tenté, endpoint d'auth, ou
        // refresh déjà en échec) : session définitivement morte → déconnexion.
        if (error?.status === 401) {
          auth.logoutLocal();
          router.navigate(['/login']);
        }
        return throwError(() => error);
      }

      // Single-flight : le PREMIER 401 lance le renouvellement, les 401
      // simultanés (isRefreshing=true) s'abonnent au même sujet et patientent.
      if (!isRefreshing) {
        isRefreshing = true;
        refreshEnCours$.next(undefined); // repasse en "en cours", purge le buffer

        auth.refreshSession().subscribe({
          next: (session) => refreshEnCours$.next(session),
          error: () => refreshEnCours$.next(null),
        });
      }

      return refreshEnCours$.pipe(
        filter((session): session is LoginResponse | null => session !== undefined),
        take(1),
        switchMap((session) => {
          isRefreshing = false;

          if (!session) {
            // Refresh impossible (token absent/expiré/rejoué) : déconnexion.
            auth.logoutLocal();
            router.navigate(['/login']);
            return throwError(() => error);
          }

          // Rejeu unique avec le nouveau JWT. Le header Authorization est
          // posé ICI (et pas par authInterceptor, déjà passé pour cette
          // requête) ; X-Auth-Retry garantit qu'un second 401 déconnecte
          // au lieu de relancer un refresh (pas de boucle infinie).
          return next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${session.token}`, 'X-Auth-Retry': '1' },
            }),
          ).pipe(finalize(() => (isRefreshing = false)));
        }),
      ) as Observable<never>;
    }),
  );
};
