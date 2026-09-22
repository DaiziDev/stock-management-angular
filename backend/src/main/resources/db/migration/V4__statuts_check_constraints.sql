-- =====================================================================
-- V4 — Contraintes CHECK des statuts, alignées sur les nouveaux enums
--
-- Les bases créées par l'ancien ddl-auto: update portent des contraintes
-- CHECK générées par Hibernate (commandeclient_statut_check,
-- commandefournisseur_statut_check) limitées aux ANCIENS statuts :
-- elles refusent EXPEDIEE/LIVREE et RECUE_PARTIELLEMENT (§3.4/§3.5).
-- On les recrée avec la liste complète des valeurs actuelles.
--
-- Les noms <table>_<colonne>_check sont la convention de nommage
-- PostgreSQL ; DROP IF EXISTS tolère leur absence (bases neuves créées
-- depuis V1, qui ne les avait pas).
-- =====================================================================

ALTER TABLE commandeclient DROP CONSTRAINT IF EXISTS commandeclient_statut_check;
ALTER TABLE commandeclient
    ADD CONSTRAINT commandeclient_statut_check
    CHECK (statut IN ('EN_COURS', 'VALIDEE', 'EXPEDIEE', 'LIVREE', 'ANNULEE'));

ALTER TABLE commandefournisseur DROP CONSTRAINT IF EXISTS commandefournisseur_statut_check;
ALTER TABLE commandefournisseur
    ADD CONSTRAINT commandefournisseur_statut_check
    CHECK (statut IN ('EN_ATTENTE', 'RECUE_PARTIELLEMENT', 'RECUE', 'ANNULEE'));
