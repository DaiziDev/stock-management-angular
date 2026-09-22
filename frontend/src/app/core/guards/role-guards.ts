import { inject } from '@angular/core';
import { type CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/services';

/**
 * Sépare les deux espaces par rôle (en complément de authGuard qui ne vérifie
 * que "quelqu'un est connecté") :
 *
 * - superAdminGuard : accès réservé au SUPER_ADMIN (console plateforme).
 *   Un compte d'entreprise est renvoyé vers son dashboard.
 * - tenantGuard : accès réservé aux comptes D'ENTREPRISE (ADMIN,
 *   GESTIONNAIRE, VENDEUR). Le SUPER_ADMIN est renvoyé vers la console
 *   plateforme -- il n'a pas d'entrepriseId, les endpoints métier
 *   (articles, dashboard...) n'auraient aucun sens pour lui.
 *
 * Complément indispensable du filtrage côté backend : la sidebar masque
 * déjà les liens par rôle (NAV.roles), mais une URL tapée à la main ne
 * doit jamais donner accès au mauvais espace.
 */

function rediriger(router: Router, url: string): ReturnType<Router['createUrlTree']> {
  return router.createUrlTree([url]);
}

export const superAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isPlatformAdmin()) {
    return true;
  }
  return rediriger(router, '/dashboard');
};

export const tenantGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isPlatformAdmin()) {
    return true;
  }
  return rediriger(router, '/plateforme');
};
