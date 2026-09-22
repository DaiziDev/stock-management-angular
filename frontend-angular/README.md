# Frontend — Application SGS

Interface utilisateur de SGS, construite avec **Angular 21** en composants standalone, **Tailwind CSS 4** pour le style et **Chart.js** pour les graphiques du tableau de bord.

## Prérequis

| Outil     | Version    |
|-----------|------------|
| Node.js   | 20 ou 22   |
| npm       | 10+        |
| Angular CLI | 21 (via `npx`, pas besoin de l'installer globalement) |

Le backend doit tourner sur **http://localhost:8081** (voir [backend/README.md](../backend/README.md)). L'URL de l'API se configure dans `src/app/environments/environment.ts`.

## Démarrage rapide

```bash
npm install
npm start
```

L'application est disponible sur **http://localhost:4200**. Le rechargement à chaud est actif à chaque modification de fichier.

Autres scripts utiles :

| Commande         | Effet                                                        |
|------------------|--------------------------------------------------------------|
| `npm run build`  | Build de production dans `dist/`                             |
| `npm run watch`  | Build en mode développement avec recompilation continue      |
| `npm test`       | Tests unitaires avec Vitest                                  |

## Connexion

Au premier démarrage du backend, un compte opérateur est créé automatiquement :

| Login             | Mot de passe |
|-------------------|--------------|
| `admin@sgs.local` | `****` (défini dans `DataInitializer.java`) |

Selon votre rôle, vous êtes dirigé vers :

- `/plateforme` : console de l'opérateur de la plateforme (SUPER_ADMIN uniquement) — onboarding des entreprises clientes et statistiques globales ;
- `/dashboard` : application d'entreprise (ADMIN, GESTIONNAIRE, VENDEUR) — catalogue, stock, commandes, ventes, clients et fournisseurs de *votre* entreprise.

## Organisation du code

```
src/app/
├── core/            Auth (JWT), guards de routes, intercepteurs
├── shared/          Briques réutilisables : composants, pipes, directives, modèles, validateurs
├── environments/    URL de l'API selon l'environnement
└── features/        Un dossier par module métier, chargé en lazy loading
    ├── auth/                  Page de connexion
    ├── dashboard/             Tableau de bord (KPIs, graphiques)
    ├── articles/              Catalogue produits
    ├── categories/            Catégories
    ├── stock (via rapports)/  Mouvements et alertes
    ├── commandes-client/      Commandes clients
    ├── commandes-fournisseur/ Commandes fournisseurs
    ├── ventes/                Point de vente
    ├── clients/               Fiches clients
    ├── fournisseurs/          Fiches fournisseurs
    ├── utilisateurs/          Gestion des comptes (ADMIN)
    ├── entreprises/           Gestion des entreprises (SUPER_ADMIN)
    ├── plateforme/            Console plateforme (SUPER_ADMIN)
    ├── rapports/              Rapports et mouvements de stock
    └── bientot/               Placeholder des modules en construction / 404
```

Chaque module de `features/` suit la même structure : un fichier de routes (`*.routes.ts`) avec lazy loading, des composants, et un service dédié aux appels API.

## Conventions

- **Composants standalone** : pas de `NgModule`, chaque composant déclare ses imports.
- **Routes paresseuses** : chaque feature est chargée via `loadChildren` depuis `app.routes.ts` ; les guards (`authGuard`, `tenantGuard`, `superAdminGuard`) protègent les routes en un seul endroit.
- **Style** : Tailwind CSS 4 (via PostCSS), utilitaires directement dans les templates ; le style global vit dans `src/styles.css`.
- **Formatage** : Prettier est configuré dans `package.json` (100 colonnes, guillemets simples, parser Angular pour les `.html`).

## Tests

Les tests unitaires tournent avec **Vitest** (et jsdom) :

```bash
npm test
```

Les fichiers de tests sont co-localisés avec le code (ex. `src/app/shared/components/icon/icon.spec.ts`).

## Build de production

```bash
npm run build
```

Le résultat est optimisé (bundling, minification, budgets) et se trouve dans `dist/frontend/`. Pensez à vérifier `environment.ts` (ou à créer un `environment.prod.ts`) pour pointer vers l'URL d'API de production.
