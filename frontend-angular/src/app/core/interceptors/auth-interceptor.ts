import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/services';

/**
 * Pose le header "Authorization: Bearer <token>" sur chaque requête sortante,
 * sauf sur /auth/login (pas encore de token à ce stade).
 * Sans cet intercepteur, JwtAuthFilter (backend) ne voit jamais le token
 * et rejette la requête comme non authentifiée.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  if (!token || req.url.includes('/auth/login')) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
