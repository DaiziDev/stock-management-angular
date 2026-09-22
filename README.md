# SGS — Système de Gestion de Stock

Backend d'une application SaaS **multi-tenant** de gestion de stock pour PME : articles, commandes fournisseurs et clients, ventes au comptoir, mouvements de stock tracés, dashboard et backoffice plateforme.

> ⚠️ Le dossier `frontend/` est actuellement vide — ce dépôt ne contient que l'API backend.

## Stack technique

| Composant | Technologie |
|---|---|
| Langage | Java 17 |
| Framework | Spring Boot 3.4.1 (Web, Data JPA, Security, Validation, Mail) |
| Base de données | PostgreSQL |
| Migrations de schéma | Flyway (source de vérité : `db/migration`) |
| Authentification | JWT (access 24 h) + refresh token opaque (7 j, rotation) |
| Documentation API | springdoc-openapi (Swagger UI) |
| Build | Maven |

## Prérequis

- **Java 17+** (JDK)
- **Maven 3.8+** (ou utiliser le wrapper si présent)
- **PostgreSQL 14+** en fonctionnement local

## Base de données

Créer la base de développement :

```sql
CREATE DATABASE stock_db;
```

Le schéma est entièrement géré par **Flyway** : au premier démarrage, les migrations `V1` à `V5` sont appliquées automatiquement (`src/main/resources/db/migration`). Hibernate fonctionne en `ddl-auto: validate` — **ne jamais modifier le schéma via les entités**, toujours ajouter une migration `V<n>__description.sql`.

## Variables d'environnement

Toutes les variables sont **optionnelles en développement** (des défauts raisonnables existent) :

| Variable | Défaut (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/stock_db` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur de la base |
| `DB_PASSWORD` | `postgres` | Mot de passe de la base |
| `JWT_SECRET` | *(secret de dev public)* | Clé de signature des JWT. **Obligatoire sans défaut en prod** — générer avec `openssl rand -base64 64` |
| `JWT_EXPIRATION` | `86400000` | Durée de vie de l'access token en ms (24 h) |
| `JWT_REFRESH_DAYS` | `7` | Durée de vie du refresh token en jours |
| `MAIL_USERNAME` | *(vide)* | Compte SMTP (Gmail) pour l'envoi des bons de commande |
| `MAIL_PASSWORD` | *(vide)* | Mot de passe / mot de passe d'application SMTP |

> 🔐 Le secret JWT par défaut est **volontairement public** (réservé au développement local). Tout token signé avec doit être considéré comme non fiable. Le profil `prod` refuse de démarrer sans `JWT_SECRET`.

## Démarrage (développement)

```bash
cd backend
mvn spring-boot:run
```

L'API démarre sur **http://localhost:8081**.

- Swagger UI : http://localhost:8081/swagger-ui.html
- OpenAPI JSON : http://localhost:8081/v3/api-docs

### Compte bootstrap

Au démarrage, `DataInitializer` crée automatiquement (idempotent) :

| Compte | Login | Mot de passe | Rôle |
|---|---|---|---|
| Opérateur plateforme | `admin@sgs.local` | `admin123` | `SUPER_ADMIN` |
| Entreprise de démo | — | — | Entreprise « SGS Demo » |

Le `SUPER_ADMIN` n'est rattaché à **aucune entreprise** : il onboard les entreprises clientes depuis `/api/plateforme` (création de l'entreprise + de son premier `ADMIN`). Le endpoint `/api/auth/register` est réservé aux `ADMIN` (dans leur propre entreprise) et au `SUPER_ADMIN`.

## Rôles

| Rôle | Périmètre |
|---|---|
| `SUPER_ADMIN` | Backoffice plateforme : stats globales, onboarding des entreprises. `entrepriseId = null` dans le JWT |
| `ADMIN` | Patron d'une entreprise cliente : gère SES utilisateurs et paramètres |
| `GESTIONNAIRE` | Commandes, fournisseurs, stock, rapports (tenant) |
| `VENDEUR` | Vente au comptoir + consultation articles/clients (tenant) |

## Tests

Les tests d'intégration (règles de stock RG-02 → RG-06, isolation multi-tenant) utilisent la **même base PostgreSQL de dev** : ils ne créent que des données préfixées `TEST-` et les suppriment après chaque test. Le profil `test` redirige le SMTP vers `localhost:2525` (les envois échouent vite et sont absorbés).

```bash
cd backend
mvn test
```

## Déploiement (production)

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Le profil `prod` (`application-prod.yaml`) durcie la configuration :

- `JWT_SECRET` est **obligatoire** (aucun défaut) — l'application refuse de démarrer sans ;
- `spring.jpa.hibernate.ddl-auto=validate` (le schéma reste maîtrisé par Flyway) ;
- logs SQL désactivés.

## Aperçu de l'API

Tous les endpoints (hors auth et Swagger) exigent le header `Authorization: Bearer <access token>`.

| Groupe | Base | Rôle |
|---|---|---|
| Authentification | `/api/auth` | `login`, `register`, `refresh`, `logout`, `me` |
| Articles | `/api/articles` | CRUD référentiel produits |
| Catégories | `/api/categories` | CRUD |
| Clients | `/api/clients` | CRUD |
| Fournisseurs | `/api/fournisseurs` | CRUD |
| Commandes fournisseur | `/api/commandes-fournisseur` | Création, réception (complète/partielle), annulation |
| Commandes client | `/api/commandes-client` | Création, validation |
| Ventes | `/api/ventes` | Création seule (décrémente le stock immédiatement, historique immuable) |
| Mouvements de stock | `/api/mouvements-stock` | Historique + ajustements manuels (motif obligatoire) |
| Stock | `/api/stock` | État, alertes de seuil, valorisation (lecture seule) |
| Dashboard | `/api/dashboard` | KPIs + graphiques 30 jours |
| Utilisateurs | `/api/utilisateurs` | Gestion des comptes |
| Entreprises | `/api/entreprises` | Paramètres de l'entreprise |
| Plateforme | `/api/plateforme` | **SUPER_ADMIN uniquement** |

### Flux de connexion

```
POST /api/auth/login { login, motDePasse }
  → 200 { accessToken, refreshToken, user }

# JWT expiré (rotation : l'ancien refresh est consommé)
POST /api/auth/refresh { refreshToken }
  → 200 { accessToken, refreshToken, user }

# Déconnexion (révoque le refresh token, idempotent)
POST /api/auth/logout { refreshToken }
```

## Règles métier clés

1. **Multi-tenancy** : chaque requête est scopée par l'`entrepriseId` du JWT (`CurrentUserService`). Les données d'une autre entreprise renvoient **404**, jamais 403 (pas de fuite d'existence).
2. **Traçabilité du stock** : `MvtStkService` est le **seul** code autorisé à modifier `Article.stockActuel`. Aucun mouvement sans trace `MvtStk` (ENTREE / SORTIE / AJUSTEMENT).
3. **Stock jamais négatif** : `StockInsuffisantException` + `@Transactional` ⇒ une vente multi-lignes est **refusée en bloc**, jamais partielle.
4. **Ventes immuables** : une vente enregistrée est un fait historique — pas d'update/delete.
5. **Réceptions partielles** : une commande fournisseur peut être reçue en plusieurs fois ; double réception refusée (le stock ne serait compté deux fois).

## Structure du projet

```
backend/
├── pom.xml                          # Dépendances Maven
└── src/
    ├── main/
    │   ├── java/com/sgs/backend/
    │   │   ├── article/             # Un package = entité + repo + service + controller + dto/
    │   │   ├── categorie/
    │   │   ├── client/
    │   │   ├── fournisseur/
    │   │   ├── commandeClient/
    │   │   ├── commandeFournisseur/
    │   │   ├── vente/               # Vente + LigneVente
    │   │   ├── mvtStk/              # Cœur du stock (mouvements)
    │   │   ├── stock/               # Vues lecture seule (état, alertes, valorisation)
    │   │   ├── dashboard/           # KPIs et graphiques
    │   │   ├── entreprise/          # Tenant
    │   │   ├── utilisateur/         # Comptes
    │   │   ├── auth/                # Refresh tokens
    │   │   ├── notification/        # Emails best-effort
    │   │   ├── plateforme/          # Backoffice SUPER_ADMIN
    │   │   ├── roles/               # UserRole
    │   │   ├── common/              # AbstractEntity, gestion d'erreurs globale
    │   │   └── config/              # Security, JWT, CORS, OpenAPI, bootstrap
    │   └── resources/
    │       ├── application.yaml     # Config dev (défauts inclus)
    │       ├── application-prod.yaml
    │       └── db/migration/        # Migrations Flyway V1 → V5
    └── test/
        └── java/com/sgs/backend/integration/   # Tests d'intégration stock
```
