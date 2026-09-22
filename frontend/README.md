# Frontend — Application SGS

Interface utilisateur de SGS : connexion, tableau de bord, catalogue articles, stock, commandes, point de vente, clients, fournisseurs, gestion des comptes et console plateforme.

| Composant | Technologie |
|---|---|
| Framework | Angular 21 (composants standalone, sans NgModule) |
| Style | Tailwind CSS 4 (via PostCSS) |
| Graphiques | Chart.js 4 |
| Tests | Vitest 4 (via le builder `@angular/build:unit-test`, jsdom) |
| Build | Angular CLI (`@angular/build`) |

## Prérequis

| Outil | Version |
|---|---|
| Node.js | 20.19+ ou 22.12+ |
| npm | 10 ou plus |

Le backend doit tourner sur http://localhost:8081 (voir [backend/README.md](../backend/README.md)). L'URL de l'API se configure dans `src/app/environments/environment.ts` (`apiUrl`, défaut `http://localhost:8081/api`).

## 1. Installation et lancement

```bash
npm install
npm start
```

L'application est disponible sur http://localhost:4200 avec rechargement à chaud.

### Scripts

| Commande | Effet |
|---|---|
| `npm start` | Serveur de développement sur http://localhost:4200 |
| `npm run build` | Build de production dans `dist/Frontend/` |
| `npm run watch` | Build en mode développement avec recompilation continue |
| `npm test` | Tests unitaires avec Vitest |

## 2. Connexion

Au premier démarrage du backend, un compte opérateur est créé automatiquement :

| Login | Mot de passe | Rôle |
|---|---|---|
| `admin@sgs.local` | `admin123` | `SUPER_ADMIN` |

Selon le rôle, l'utilisateur est dirigé vers :

- `/plateforme` : console de l'opérateur de la plateforme (SUPER_ADMIN uniquement) — onboarding des entreprises clientes et statistiques globales ;
- `/dashboard` : application d'entreprise (ADMIN, GESTIONNAIRE, VENDEUR) — catalogue, stock, commandes, ventes, clients et fournisseurs de l'entreprise connectée.

## Organisation du code

```
src/app/
├── core/            Auth (JWT), guards de routes, intercepteurs, layout, modèles
├── shared/          Briques réutilisables : composants, pipes, directives, validateurs
├── environments/    URL de l'API selon l'environnement
└── features/        Un dossier par module métier, chargé en lazy loading
    ├── auth/                  Page de connexion
    ├── dashboard/             Tableau de bord (KPIs, graphiques)
    ├── articles/              Catalogue produits
    ├── categories/            Catégories
    ├── commandes-client/      Commandes clients (validation, expédition, livraison)
    ├── commandes-fournisseur/ Commandes fournisseurs (réception complète ou partielle)
    ├── ventes/                Point de vente
    ├── clients/               Fiches clients
    ├── fournisseurs/          Fiches fournisseurs
    ├── rapports/              État du stock, alertes, valorisation, mouvements
    ├── utilisateurs/          Gestion des comptes (ADMIN)
    ├── entreprises/           Gestion des entreprises (SUPER_ADMIN)
    ├── plateforme/            Console plateforme (SUPER_ADMIN)
    └── bientot/               Placeholder des modules en construction / 404
```

Chaque module de `features/` suit la même structure : un fichier de routes (`*.routes.ts`) avec lazy loading, des composants, et un service dédié aux appels API.

## Conventions

- Composants standalone : chaque composant déclare ses imports, pas de `NgModule`.
- Routes paresseuses : chaque feature est chargée via `loadChildren` depuis `app.routes.ts` ; les guards (`authGuard`, `superAdminGuard`, `tenantGuard`) protègent les routes en un seul endroit.
- Style : Tailwind CSS 4, utilitaires directement dans les templates ; le style global vit dans `src/styles.css`.
- Formatage : Prettier configuré dans `package.json` (100 colonnes, guillemets simples, parser Angular pour les `.html`).

## Tests

```bash
npm test
```

Tests unitaires avec Vitest et jsdom. Les fichiers de tests sont co-localisés avec le code (`*.spec.ts`, par exemple `src/app/shared/components/icon/icon.spec.ts`).

## Build de production

```bash
npm run build
```

Résultat optimisé (bundling, minification, hashing, budgets de taille) dans `dist/Frontend/`. Pointer `apiUrl` de `environment.ts` vers l'URL d'API de production avant le build : aucune substitution de fichier n'est configurée dans `angular.json`, `environment.ts` est donc utilisé tel quel en production ; `environment.development.ts` existe mais est vide et non importé.
