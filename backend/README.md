# Backend — API SGS

API REST du système de gestion de stock SGS, construite avec **Spring Boot 3.4** et **Java 17**. Elle expose tout ce dont l'application frontend a besoin : authentification JWT, catalogue articles, stock, commandes, ventes et backoffice plateforme.

## Prérequis

| Outil       | Version         |
|-------------|-----------------|
| Java (JDK)  | 17 ou plus      |
| Maven       | 3.8+            |
| PostgreSQL  | 14+             |

> Le wrapper Maven du dépôt est incomplet (fichier `.mvn/wrapper/maven-wrapper.properties` manquant). Utilisez un Maven installé sur la machine (`mvn`), ou restaurez le wrapper avant d'utiliser `./mvnw`.

## Démarrage rapide

### 1. Préparer la base de données

Par défaut, l'application se connecte à `jdbc:postgresql://localhost:5432/stock_db` avec l'utilisateur `postgres`. Définissez vous-même le mot de passe de votre choix :

```bash
# Remplacez les étoiles par VOTRE mot de passe
sudo -u postgres psql -c "ALTER USER postgres PASSWORD '****';"
sudo -u postgres createdb stock_db
```

Reportez ce même mot de passe dans la variable `DB_PASSWORD` au lancement de l'application (voir [Configuration](#configuration)).

Vérifiez que la connexion TCP fonctionne (c'est ainsi que l'API se connecte) :

```bash
PGPASSWORD=**** psql -h localhost -U postgres -d stock_db -c "SELECT 1;"
```

### 2. Lancer l'application

```bash
mvn spring-boot:run
```

L'API écoute sur **http://localhost:8081**.

- Swagger UI : http://localhost:8081/swagger-ui/index.html
- OpenAPI JSON : http://localhost:8081/v3/api-docs

### 3. Se connecter

Un compte SUPER_ADMIN de « bootstrap » est créé automatiquement au premier démarrage :

| Login             | Mot de passe |
|-------------------|--------------|
| `admin@sgs.local` | `****`       |

C'est le compte opérateur de la plateforme : il sert à onboarder les entreprises clientes depuis la console `/plateforme`. Le login et le mot de passe par défaut sont définis dans `DataInitializer.java` (constantes `BOOTSTRAP_*`) : personnalisez-les avant toute mise en production.

## Configuration

Toute la configuration par défaut vit dans `src/main/resources/application.yaml` et se surcharge par variables d'environnement :

| Variable          | Défaut                                        | Rôle                                   |
|-------------------|-----------------------------------------------|----------------------------------------|
| `DB_URL`          | `jdbc:postgresql://localhost:5432/stock_db`   | URL JDBC PostgreSQL                    |
| `DB_USERNAME`     | `postgres`                                    | Utilisateur base de données            |
| `DB_PASSWORD`     | `****` (défaut de dev)                        | Mot de passe base de données           |
| `JWT_SECRET`      | clé de développement (publique, volontairement) | Clé de signature des tokens JWT     |
| `JWT_EXPIRATION`  | `86400000` (24 h)                             | Durée de vie de l'access token (ms)    |
| `JWT_REFRESH_DAYS`| `7`                                           | Durée de vie du refresh token (jours)  |
| `MAIL_USERNAME`   | vide                                          | Compte SMTP (Gmail) pour les e-mails   |
| `MAIL_PASSWORD`   | vide                                          | Mot de passe / mot d'application SMTP  |

Le profil **prod** (`application-prod.yaml`) durcit tout ça : `JWT_SECRET` y est obligatoire et sans valeur par défaut.

## Base de données et migrations

Le schéma est géré par **Flyway**, qui est la source de vérité :

- Les migrations vivent dans `src/main/resources/db/migration/` (`V1__baseline_schema.sql`, `V2`... `V5`).
- Hibernate est en `ddl-auto: validate` : il ne crée ni ne modifie rien, il vérifie seulement la cohérence des entités avec le schéma.
- Pour faire évoluer le schéma, ajoutez un fichier `V6__description_de_la_change.sql`. Ne touchez jamais au schéma à la main.
- Sur une base existante créée avant Flyway, `baseline-on-migrate` pose automatiquement le marqueur « V1 appliquée ».

## Sécurité

- Authentification **JWT** : access token + refresh token (révocable, stocké côté serveur).
- Quatre rôles hiérarchisés (voir `UserRole`) :
  - `SUPER_ADMIN` : opérateur de la plateforme, rattaché à aucune entreprise, gère les entreprises clientes ;
  - `ADMIN` : patron d'une entreprise cliente, gère ses utilisateurs ;
  - `GESTIONNAIRE` : commandes, fournisseurs, stock, rapports ;
  - `VENDEUR` : ventes au comptoir et consultation.
- Cloisonnement multi-tenant : chaque requête est filtrée par l'`entrepriseId` porté dans le JWT.
- Endpoints publics : `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, Swagger UI et la documentation OpenAPI.

## Organisation du code

```
src/main/java/com/sgs/backend/
├── config/          Sécurité (JWT, CORS), Swagger, bootstrap des données
├── entreprise/      Entreprises clientes (multi-tenant)
├── utilisateur/     Comptes utilisateurs
├── roles/           Énumération des rôles
├── article/         Catalogue produits
├── categorie/       Catégories
├── stock/           Niveaux de stock (lecture)
├── mvtStk/          Mouvements de stock (entrées/sorties/ajustements)
├── commandeClient/  Commandes clients (validation -> sortie de stock)
├── commandeFournisseur/  Commandes d'achat (réception -> entrée de stock)
├── vente/           Ventes au comptoir
├── client/          Fiches clients
├── fournisseur/     Fiches fournisseurs
├── dashboard/       KPIs et données de graphiques
├── notification/    Alertes in-app (stock sous seuil)
└── plateforme/      Backoffice SUPER_ADMIN (stats, onboarding)
```

Chaque module suit le même découpage : `Controller` (endpoints + annotations OpenAPI), `Service` (logique métier), `Repository` (accès données), DTOs de requête/réponse.

## Tests et vérifications

```bash
mvn test          # tests unitaires et d'intégration
mvn verify        # tests + vérifications
```

Avant de pousser, vérifiez au minimum que le projet compile :

```bash
mvn compile
```
