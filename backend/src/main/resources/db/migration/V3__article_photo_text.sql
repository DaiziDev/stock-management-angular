-- =====================================================================
-- V3 — article.photo en TEXT (photos stockées en data URL base64)
--
-- Reprend la responsabilité de DataInitializer.elargirColonnePhotoSiNecessaire()
-- : avec ddl-auto: validate, Hibernate valide le schéma AVANT que le
-- CommandLineRunner ne s'exécute — une base encore en varchar(255)
-- refuserait donc de démarrer. Passée par Flyway, la colonne est élargie
-- AVANT la validation.
--
-- Idempotent : sur une base neuve (V1 crée déjà photo en TEXT) ou déjà
-- élargie, ALTER COLUMN TYPE vers le même type est un no-op inoffensif.
-- =====================================================================

ALTER TABLE article ALTER COLUMN photo TYPE text;
