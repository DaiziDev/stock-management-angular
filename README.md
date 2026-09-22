# SGS — Système de Gestion de Stock

Application SaaS multi-tenant de gestion de stock pour PME : catalogue articles, commandes fournisseurs et clients, ventes au comptoir, mouvements de stock tracés, dashboard et backoffice plateforme.

Le dépôt contient deux applications :

## Prérequis

| Outil | Version |
|---|---|
| JDK | 17 ou plus |
| Maven | 3.8 ou plus |
| PostgreSQL | 14 ou plus |
| Node.js | 20.19+ ou 22.12+ |
| npm | 10 ou plus |

Le wrapper Maven (`backend/mvnw`) télécharge automatiquement la distribution Maven 3.9.9 au premier lancement ; aucun Maven n'a besoin d'être installé si le JDK est présent.

## 1. Base de données

Créer la base de développement :

```sql
CREATE DATABASE stock_db;
```

Par défaut l'API se connecte à `jdbc:postgresql://localhost:5432/stock_db` avec l'utilisateur `postgres` et le mot de passe `postgres`. Le schéma est entièrement géré par Flyway : les migrations `V1` à `V5` (`backend/src/main/resources/db/migration`) sont appliquées automatiquement au premier démarrage. Hibernate fonctionne en `ddl-auto: validate` : toute évolution du schéma passe par une nouvelle migration `V<n>__description.sql`, jamais par les entités.

## 2. Backend

### Configuration

Toutes les variables sont optionnelles en développement (défauts dans `backend/src/main/resources/application.yaml`) :

| Variable | Défaut (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/stock_db` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur de la base |
| `DB_PASSWORD` | `postgres` | Mot de passe de la base |
| `JWT_SECRET` | secret de dev public | Clé de signature des JWT. Obligatoire sans défaut en production (générer avec `openssl rand -base64 64`) |
| `JWT_EXPIRATION` | `86400000` | Durée de vie de l'access token en ms (24 h) |
| `JWT_REFRESH_DAYS` | `7` | Durée de vie du refresh token en jours |
| `MAIL_USERNAME` | vide | Compte SMTP (Gmail) pour l'envoi des bons de commande |
| `MAIL_PASSWORD` | vide | Mot de passe d'application SMTP |

### Lancement

```bash
cd backend
mvn spring-boot:run
```

L'API démarre sur http://localhost:8081.

- Swagger UI : http://localhost:8081/swagger-ui.html
- OpenAPI JSON : http://localhost:8081/api-docs

### Comptes créés au démarrage

`DataInitializer` crée de façon idempotente :

| Compte | Login | Mot de passe | Rôle |
|---|---|---|---|
| Opérateur plateforme | `admin@sgs.local` | `admin123` | `SUPER_ADMIN` |
| Entreprise de démonstration | — | — | Entreprise « SGS Demo » |

Le `SUPER_ADMIN` n'est rattaché à aucune entreprise : il onboard les entreprises clientes depuis la console `/plateforme` (création de l'entreprise puis de son premier `ADMIN`). Le endpoint `/api/auth/register` est réservé aux `ADMIN` (dans leur propre entreprise) et au `SUPER_ADMIN`. Le login et le mot de passe bootstrap sont définis dans `DataInitializer.java` et doivent être modifiés avant toute mise en production.

### Rôles

| Rôle | Périmètre |
|---|---|
| `SUPER_ADMIN` | Backoffice plateforme : statistiques globales, onboarding des entreprises. `entrepriseId = null` dans le JWT |
| `ADMIN` | Gestion de son entreprise : utilisateurs et paramètres |
| `GESTIONNAIRE` | Commandes, fournisseurs, stock, rapports |
| `VENDEUR` | Vente au comptoir et consultation articles/clients |

## 3. Frontend

Le frontend attend l'API sur http://localhost:8081. L'URL se configure dans `frontend/src/app/environments/environment.ts` (`apiUrl`, défaut `http://localhost:8081/api`).

```bash
cd frontend
npm install
npm start
```

L'application est disponible sur http://localhost:4200 avec rechargement à chaud. À la connexion, l'utilisateur est dirigé vers `/plateforme` (SUPER_ADMIN) ou `/dashboard` (ADMIN, GESTIONNAIRE, VENDEUR) selon son rôle.

## Tests

### Backend

```bash
cd backend
mvn test
```

Les tests d'intégration (règles de stock RG-02 à RG-06, isolation multi-tenant) utilisent la même base PostgreSQL de développement : ils ne créent que des données préfixées `TEST-` et les suppriment après chaque test. Le profil `test` redirige le SMTP vers `localhost:2525`.

### Frontend

```bash
cd frontend
npm test
```

Tests unitaires avec Vitest et jsdom. Les fichiers de tests sont co-localisés avec le code (`*.spec.ts`).

## Build de production

### Backend

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Le profil `prod` (`application-prod.yaml`) durcit la configuration : `JWT_SECRET` obligatoire (l'application refuse de démarrer sans), `ddl-auto: validate`, logs SQL désactivés.

### Frontend

```bash
cd frontend
npm run build
```

Résultat optimisé dans `dist/Frontend/`. Pointer `apiUrl` vers l'URL d'API de production avant le build.

## Aperçu de l'API

Tous les endpoints (hors authentification et Swagger) exigent le header `Authorization: Bearer <access token>`.

| Groupe | Base | Contenu |
|---|---|---|
| Authentification | `/api/auth` | `login`, `register`, `refresh`, `logout`, `me` |
| Articles | `/api/articles` | CRUD référentiel produits |
| Catégories | `/api/categories` | CRUD |
| Clients | `/api/clients` | CRUD |
| Fournisseurs | `/api/fournisseurs` | CRUD |
| Commandes fournisseur | `/api/commandes-fournisseur` | Création, réception complète ou partielle, annulation |
| Commandes client | `/api/commandes-client` | Création, validation |
| Ventes | `/api/ventes` | Création seule (décrémente le stock, historique immuable) |
| Mouvements de stock | `/api/mouvements-stock` | Historique et ajustements manuels (motif obligatoire) |
| Stock | `/api/stock` | État, alertes de seuil, valorisation (lecture seule) |
| Dashboard | `/api/dashboard` | KPIs et graphiques 30 jours |
| Utilisateurs | `/api/utilisateurs` | Gestion des comptes |
| Entreprises | `/api/entreprises` | Paramètres de l'entreprise |
| Plateforme | `/api/plateforme` | SUPER_ADMIN uniquement |

Flux d'authentification :

```
POST /api/auth/login { login, motDePasse }
  → 200 { accessToken, refreshToken, user }

POST /api/auth/refresh { refreshToken }
  → 200 { accessToken, refreshToken, user }   (rotation : l'ancien refresh est consommé)

POST /api/auth/logout { refreshToken }
  → révoque le refresh token, idempotent
```

## Règles métier clés

1. Multi-tenancy : chaque requête est scopée par l'`entrepriseId` du JWT. Les données d'une autre entreprise renvoient 404, jamais 403.
2. Traçabilité : `MvtStkService` est le seul code autorisé à modifier `Article.stockActuel` ; aucun mouvement sans trace `MvtStk` (ENTREE / SORTIE / AJUSTEMENT).
3. Stock jamais négatif : une vente multi-lignes est refusée en bloc si le stock est insuffisant, jamais partiellement appliquée.
4. Ventes immuables : une vente enregistrée est un fait historique, sans update ni delete.
5. Réceptions partielles : une commande fournisseur peut être reçue en plusieurs fois ; une double réception est refusée.

## Structure du projet

```
.
├── backend/
│   ├── pom.xml                                 # Dépendances Maven
│   └── src/
│       ├── main/java/com/sgs/backend/          # Un package par domaine métier
│       │   ├── article/ categorie/ client/ fournisseur/
│       │   ├── commandeClient/ commandeFournisseur/ vente/
│       │   ├── mvtStk/                         # Cœur du stock (mouvements)
│       │   ├── stock/ dashboard/               # Vues lecture seule, KPIs
│       │   ├── entreprise/ utilisateur/ auth/  # Tenants, comptes, tokens
│       │   ├── plateforme/                     # Backoffice SUPER_ADMIN
│       │   ├── common/ config/                 # Erreurs globales, sécurité, bootstrap
│       │   └── ...
│       ├── main/resources/
│       │   ├── application.yaml                # Config dev (défauts inclus)
│       │   ├── application-prod.yaml           # Config production
│       │   └── db/migration/                   # Migrations Flyway V1 → V5
│       └── test/java/com/sgs/backend/integration/
└── frontend/
    ├── package.json
    └── src/app/
        ├── core/                               # Auth JWT, guards, intercepteurs, layout
        ├── shared/                             # Composants, pipes, directives réutilisables
        ├── environments/                       # URL de l'API par environnement
        └── features/                           # Un dossier par module, lazy loading
            ├── auth/ dashboard/ articles/ categories/
            ├── commandes-client/ commandes-fournisseur/ ventes/
            ├── clients/ fournisseurs/ rapports/
            └── utilisateurs/ entreprises/ plateforme/
```

Chaque module backend suit le découpage `Controller` / `Service` / `Repository` / DTOs ; chaque module frontend contient ses routes en lazy loading, ses composants et son service d'appels API.
