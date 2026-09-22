/**
 * Modèles du cœur de l'application (issue de la maquette maquette.html).
 * Les rôles et la structure de navigation alimentent la sidebar et les guards.
 */

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'VENDEUR' | 'GESTIONNAIRE';

/** Les rôles "à l'intérieur d'une entreprise cliente" (tenant). */
export const ALL_ROLES: UserRole[] = ['ADMIN', 'VENDEUR', 'GESTIONNAIRE'];

export const ROLE_LABELS: Record<UserRole, string> = {
  SUPER_ADMIN: 'Plateforme',
  ADMIN: 'Administrateur',
  VENDEUR: 'Vendeur',
  GESTIONNAIRE: 'Gestionnaire',
};

export interface CurrentUser {
  id: number;
  nom: string;
  role: UserRole;
  entrepriseId: number | null;
  entrepriseName?: string | null;
  login?: string;
}

/**
 * Forme de la réponse POST /api/auth/login et /api/auth/refresh côté backend
 * (AuthTokensDTO). Le refresh token (§6.3) sert à obtenir un nouveau couple
 * de tokens sans redemander les identifiants quand le JWT de 24h expire.
 */
export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: {
    id: number;
    nom: string;
    prenom: string;
    login: string;
    role: UserRole;
    entrepriseId: number | null;
    entrepriseNom: string | null;
  };
}

export interface NavItem {
  key: string;
  label: string;
  icon: string;
  route: string;
  /** ⭐ Rôles autorisés à voir cet item dans le menu (et à accéder à sa route). */
  roles: UserRole[];
}

export interface NavGroup {
  group: string;
  items: NavItem[];
}

/**
 * Structure du menu — identique à la maquette (groupes + items),
 * enrichie d'un filtre `roles` pour le RBAC côté frontend.
 *
 * Deux espaces distincts partageant le même socle visuel :
 * - groupe "Plateforme" : console de l'opérateur (SUPER_ADMIN uniquement) ;
 * - les autres groupes : l'application d'entreprise (tenants), où le
 *   SUPER_ADMIN n'a rien à faire (garde tenantGuard côté routes).
 */
export const NAV: NavGroup[] = [
  {
    group: 'Plateforme',
    items: [
      { key: 'plateformeAccueil', label: "Vue d'ensemble", icon: 'layout-dashboard', route: '/plateforme', roles: ['SUPER_ADMIN'] },
      { key: 'plateformeEntreprises', label: 'Entreprises clientes', icon: 'building-2', route: '/plateforme/entreprises', roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    group: "Vue d'ensemble",
    items: [
      { key: 'dashboard', label: 'Tableau de bord', icon: 'layout-dashboard', route: '/dashboard', roles: ALL_ROLES },
      { key: 'stockReport', label: 'Rapports & stock', icon: 'bar-chart-3', route: '/rapports', roles: ['ADMIN', 'GESTIONNAIRE'] },
    ],
  },
  {
    group: 'Catalogue',
    items: [
      { key: 'categories', label: 'Catégories', icon: 'tag', route: '/categories', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'articles', label: 'Articles', icon: 'package', route: '/articles', roles: ALL_ROLES },
    ],
  },
  {
    group: 'Organisation',
    items: [
      { key: 'utilisateurs', label: 'Utilisateurs & rôles', icon: 'users', route: '/utilisateurs', roles: ['ADMIN'] },
    ],
  },
  {
    group: 'Tiers',
    items: [
      { key: 'clients', label: 'Clients', icon: 'user', route: '/clients', roles: ALL_ROLES },
      { key: 'fournisseurs', label: 'Fournisseurs', icon: 'truck', route: '/fournisseurs', roles: ['ADMIN', 'GESTIONNAIRE'] },
    ],
  },
  {
    group: 'Transactions',
    items: [
      { key: 'commandesClient', label: 'Commandes client', icon: 'clipboard-list', route: '/commandes-client', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'commandesFournisseur', label: 'Commandes fournisseur', icon: 'clipboard-check', route: '/commandes-fournisseur', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'mouvementsStock', label: 'Mouvements de stock', icon: 'arrow-left-right', route: '/mouvements-stock', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'ventes', label: 'Point de vente', icon: 'shopping-cart', route: '/ventes', roles: ALL_ROLES },
    ],
  },
];
