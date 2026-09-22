package com.sgs.backend.commande;

/**
 * Cycle de vie d'une commande client (§3.4 du cahier des charges) :
 * EN_COURS  -> VALIDEE   (déclenche les sorties de stock, irréversible)
 * VALIDEE   -> EXPEDIEE  (marchandise remise au transporteur, aucun impact stock)
 * EXPEDIEE  -> LIVREE    (livraison confirmée chez le client, aucun impact stock)
 * EN_COURS  -> ANNULEE   (aucun impact sur le stock, rien n'a encore bougé)
 *
 * Pas de retour possible depuis VALIDEE : annuler une commande déjà validée
 * nécessiterait de générer des mouvements de stock inverses (un "avoir"),
 * ce qui est hors périmètre pour l'instant. EXPEDIEE et LIVREE ne font que
 * préciser où en est la livraison : le stock a déjà bougé à la validation.
 */
public enum StatutCommandeClient {
    EN_COURS,
    VALIDEE,
    EXPEDIEE,
    LIVREE,
    ANNULEE
}
