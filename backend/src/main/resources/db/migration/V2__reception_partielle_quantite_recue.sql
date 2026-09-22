-- =====================================================================
-- V2 — Réception partielle des commandes fournisseur (§3.5)
--
-- Suivi, ligne par ligne, de la quantité déjà réceptionnée. La valeur
-- 0 pour les lignes existantes est correcte : rien n'était suivi avant
-- cette évolution, donc aucune réception passée n'est à rattraper.
--
-- "IF NOT EXISTS" : filet de sécurité pour les bases de dev qui ont
-- démarré pendant la transition ddl-auto "update" -> "validate" (Hibernate
-- avait pu créer la colonne avant Flyway). Inoffensif ailleurs.
-- =====================================================================

ALTER TABLE lignecommandefournisseur
    ADD COLUMN IF NOT EXISTS quantiterecue integer NOT NULL DEFAULT 0;
