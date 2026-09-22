# Backend — API SGS

API REST du système de gestion de stock SGS : authentification JWT, catalogue articles, stock tracé par mouvements, commandes fournisseurs et clients, ventes, dashboard et backoffice plateforme.

| Composant | Technologie |
|---|---|
| Langage | Java 17 |
| Framework | Spring Boot 3.4.1 (Web, Data JPA, Security, Validation, Mail) |
| Base de données | PostgreSQL |
| Migrations de schéma | Flyway (source de vérité : `db/migration`) |
| Authentification | JWT (access 24 h) + refresh token opaque (7 j, rotation) |
| Documentation API | springdoc-openapi (Swagger UI) |
| Build | Maven (wrapper 3.3.4, distribution 3.9.9 téléchargée automatiquement) |

## Prérequis

| Outil | Version |
|---|---|
| JDK | 17 ou plus |
| PostgreSQL | 14 ou plus |

Le wrapper Maven (`mvnw` / `mvnw.cmd`) télécharge Maven 3.9.9 au premier lancement : aucun Maven n'a besoin d'être installé. Sous Windows, utiliser `mvnw.cmd` (PowerShell) ou `./mvnw` (Git Bash).

## 1. Base de données

Par défaut, l'application se connecte à `jdbc:postgresql://localhost:5432/stock_db` avec l'utilisateur `postgres` et le mot de passe `postgres` :

```bash
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
sudo -u postgres createdb stock_db
```

Avec un mot de passe différent, le surcharger par la variable `DB_PASSWORD` (voir [Configuration](#configuration)).

Le schéma est entièrement géré par Flyway : les migrations `V1` à `V5` (`src/main/resources/db/migration`) sont appliquées automatiquement au premier démarrage. Hibernate fonctionne en `ddl-auto: validate` : toute évolution du schéma passe par une nouvelle migration `V<n>__description.sql`, jamais par les entités. Sur une base existante créée avant Flyway, `baseline-on-migrate` pose automatiquement le marqueur « V1 appliquée ».

## 2. Configuration

Tous les défauts vivent dans `src/main/resources/application.yaml` et se surchargent par variables d'environnement :

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

Le profil `prod` (`application-prod.yaml`) durcie la configuration : `JWT_SECRET` obligatoire (l'application refuse de démarrer sans), `ddl-auto: validate`, logs SQL désactivés.

## 3. Lancement

```bash
./mvnw spring-boot:run
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

Le `SUPER_ADMIN` n'est rattaché à aucune entreprise : il onboard les entreprises clientes depuis la console `/plateforme` (création de l'entreprise puis de son premier `ADMIN`). Le endpoint `/api/auth/register` est réservé aux `ADMIN` (dans leur propre entreprise) et au `SUPER_ADMIN`. Le login et le mot de passe bootstrap sont définis dans `DataInitializer.java` (constantes `BOOTSTRAP_*`) et doivent être modifiés avant toute mise en production.

## Sécurité

- Authentification JWT : access token (24 h) + refresh token opaque (7 j, révocable, stocké côté serveur, rotation à chaque `refresh`).
- Quatre rôles hiérarchisés (`UserRole`) :

| Rôle | Périmètre |
|---|---|
| `SUPER_ADMIN` | Backoffice plateforme : statistiques globales, onboarding des entreprises. `entrepriseId = null` dans le JWT |
| `ADMIN` | Gestion de son entreprise : utilisateurs et paramètres |
| `GESTIONNAIRE` | Commandes, fournisseurs, stock, rapports |
| `VENDEUR` | Vente au comptoir et consultation articles/clients |

- Cloisonnement multi-tenant : chaque requête est scopée par l'`entrepriseId` du JWT. Les données d'une autre entreprise renvoient 404, jamais 403.
- Endpoints publics : `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, Swagger UI et documentation OpenAPI. Tous les autres exigent le header `Authorization: Bearer <access token>`.

## Aperçu des modules

Un package = une entité ou un domaine, avec le même découpage : `Controller` (endpoints + annotations OpenAPI), `Service` (logique métier), `Repository` (accès données), DTOs de requête/réponse.

| Package | Contenu |
|---|---|
| `article/` `categorie/` | Catalogue produits et catégories |
| `client/` `fournisseur/` | Fiches clients et fournisseurs |
| `commandeClient/` `commandeFournisseur/` | Commandes de vente et d'achat (validation, réception complète ou partielle, annulation) |
| `commande/` | Énumérations de statut partagées des deux types de commandes |
| `ligneCommandeClient/` `ligneCommandeFournisseur/` `ligneVente/` | Lignes de détail des commandes et ventes |
| `vente/` | Ventes au comptoir (immuables, décrémentent le stock) |
| `mvtStk/` | Cœur du stock : mouvements ENTREE / SORTIE / AJUSTEMENT |
| `stock/` | État, alertes de seuil, valorisation (lecture seule) |
| `dashboard/` | KPIs et graphiques 30 jours |
| `entreprise/` | Tenants (entreprises clientes) |
| `utilisateur/` | Comptes utilisateurs |
| `auth/` | Refresh tokens |
| `notification/` | Alertes in-app (stock sous seuil, calculées à la volée) et emails best-effort |
| `plateforme/` | Backoffice SUPER_ADMIN (stats, onboarding) |
| `adresse/` | Composant `@Embeddable` réutilisé par les entités |
| `common/` `config/` | Entité de base, gestion d'erreurs globale ; sécurité JWT, CORS, OpenAPI, bootstrap |

## Tests

```bash
./mvnw test
```

Les tests d'intégration (règles de stock RG-02 à RG-06, isolation multi-tenant) utilisent la même base PostgreSQL de développement : ils ne créent que des données préfixées `TEST-` et les suppriment après chaque test. Le profil `test` (`src/test/resources/application-test.yaml`) redirige le SMTP vers `localhost:2525`.

Avant de pousser, vérifier au minimum la compilation :

```bash
./mvnw compile
```

## Build de production

```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Structure du projet

```
backend/
├── mvnw / mvnw.cmd / .mvn/wrapper/          # Wrapper Maven (télécharge Maven 3.9.9)
├── pom.xml                                  # Dépendances Maven
└── src/
    ├── main/
    │   ├── java/com/sgs/backend/            # Un package par domaine métier
    │   │   ├── article/ categorie/ client/ fournisseur/
    │   │   ├── commandeClient/ commandeFournisseur/ vente/
    │   │   ├── mvtStk/                      # Cœur du stock (mouvements)
    │   │   ├── stock/ dashboard/            # Vues lecture seule, KPIs
    │   │   ├── entreprise/ utilisateur/ auth/
    │   │   ├── notification/ plateforme/
    │   │   └── common/ config/
    │   └── resources/
    │       ├── application.yaml             # Config dev (défauts inclus)
    │       ├── application-prod.yaml        # Config production
    │       └── db/migration/                # Migrations Flyway V1 → V5
    └── test/
        ├── java/com/sgs/backend/integration/  # Tests d'intégration stock
        └── resources/application-test.yaml
```
