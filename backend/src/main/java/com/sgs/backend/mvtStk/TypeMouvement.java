package com.sgs.backend.mvtStk;

/**
 * ENTREE      : réception d'une commande fournisseur -> stock += quantite
 * SORTIE      : validation d'une commande client ou une vente -> stock -= quantite
 * AJUSTEMENT  : correction manuelle (inventaire), quantite signée (+/-),
 *               toujours accompagnée d'un motif (RG-06 du cahier des charges)
 */
public enum TypeMouvement {
    ENTREE,
    SORTIE,
    AJUSTEMENT
}
