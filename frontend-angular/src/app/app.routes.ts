import { type Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { superAdminGuard, tenantGuard } from './core/guards/role-guards';
import { Login } from './features/auth/login';
import { BientotComponent } from './features/bientot/bientot.component';
// ⚠️ Le placeholder BientotComponent reste utilisé par la route 404 (**).

/**
 * Toutes les routes applicatives (hors /login) sont enfants d'une route vide
 * gardée par authGuard : ça protège chaque fonctionnalité en un seul endroit,
 * plutôt que de devoir répéter `canActivate: [authGuard]` sur chacune (et
 * risquer d'en oublier une, comme c'était le cas avant).
 *
 * Deux espaces distincts sous le même socle visuel :
 * - /plateforme  : console de l'opérateur (SUPER_ADMIN uniquement) ;
 * - le reste     : application d'entreprise (tenants), le SUPER_ADMIN y est
 *   redirigé par tenantGuard.
 */
export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'plateforme',
        canActivate: [superAdminGuard],
        loadChildren: () =>
          import('./features/plateforme/plateforme.routes').then((m) => m.PLATEFORME_ROUTES),
      },
      {
        path: 'categories',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/categories/categorie.routes').then((m) => m.CATEGORIE_ROUTES),
      },
      {
        path: 'articles',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/articles/article.routes').then((m) => m.ARTICLE_ROUTES),
      },
      {
        path: 'dashboard',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'entreprises',
        redirectTo: '/plateforme/entreprises',
        pathMatch: 'full',
      },
      {
        path: 'clients',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/clients/client.routes').then((m) => m.CLIENT_ROUTES),
      },
      {
        path: 'fournisseurs',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/fournisseurs/fournisseur.routes').then((m) => m.FOURNISSEUR_ROUTES),
      },

      // ── Modules annoncés dans le menu, encore en construction ──
      // Un placeholder propre plutôt qu'une page blanche.
      {
        path: 'rapports',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/rapports/rapports.routes').then((m) => m.RAPPORTS_ROUTES),
      },
      {
        path: 'utilisateurs',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/utilisateurs/utilisateur.routes').then((m) => m.UTILISATEUR_ROUTES),
      },
      {
        path: 'commandes-client',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/commandes-client/commande-client.routes').then((m) => m.COMMANDE_CLIENT_ROUTES),
      },
      {
        path: 'commandes-fournisseur',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/commandes-fournisseur/commande-fournisseur.routes').then((m) => m.COMMANDE_FOURNISSEUR_ROUTES),
      },
      {
        path: 'mouvements-stock',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/rapports/rapports.routes').then((m) => m.MOUVEMENTS_ROUTES),
      },
      {
        path: 'ventes',
        canActivate: [tenantGuard],
        loadChildren: () =>
          import('./features/ventes/vente.routes').then((m) => m.VENTE_ROUTES),
      },

      // 404 : toute URL inconnue, avec respect de l'espace du rôle
      // (tenantGuard renvoie le superadmin vers la console plateforme).
      {
        path: '**',
        canActivate: [tenantGuard],
        component: BientotComponent,
        data: { titre: 'Page introuvable' },
      },
    ],
  },
];
