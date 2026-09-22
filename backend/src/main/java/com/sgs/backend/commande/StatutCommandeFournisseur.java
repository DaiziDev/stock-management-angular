package com.sgs.backend.commande;

/**
 * Cycle de vie d'une commande fournisseur (§3.5 du cahier des charges) :
 * EN_ATTENTE -> RECUE                   (réception complète : une entrée de stock
 *                                        pour le solde restant de chaque ligne, irréversible)
 * EN_ATTENTE / RECUE_PARTIELLEMENT -> RECUE_PARTIELLEMENT
 *                                        (réception partielle : entrées de stock limitées
 *                                        aux quantités effectivement reçues)
 * RECUE_PARTIELLEMENT -> RECUE          (réception du solde restant)
 * EN_ATTENTE -> ANNULEE                 (rien n'a encore bougé)
 *
 * Une commande RECUE ne peut pas être réceptionnée une seconde fois --
 * le stock serait compté deux fois (règle explicite du flux fonctionnel).
 * Une commande RECUE_PARTIELLEMENT ne peut pas être annulée : une partie du
 * stock a déjà bougé, l'annulation nécessiterait des mouvements inverses.
 */
public enum StatutCommandeFournisseur {
    EN_ATTENTE,
    RECUE_PARTIELLEMENT,
    RECUE,
    ANNULEE
}
