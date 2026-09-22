package com.sgs.backend.roles;

/**
 * Énumération des rôles utilisateur dans le système SGS.
 *
 * SUPER_ADMIN : Opérateur de la PLATEFORME (nous) — onboard les entreprises
 *               clientes, crée leur premier ADMIN. N'est rattaché à aucune
 *               entreprise (entrepriseId null dans le JWT) : c'est ça qui le
 *               distingue structurellement d'un compte client.
 * ADMIN       : Patron d'une entreprise cliente — gère SES utilisateurs et
 *               SES paramètres, ne voit que les données de son entreprise
 * GESTIONNAIRE: Gère les commandes, fournisseurs, stock, rapports (tenant)
 * VENDEUR     : Vente au comptoir + consultation articles/clients (tenant)
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    GESTIONNAIRE,
    VENDEUR
}
