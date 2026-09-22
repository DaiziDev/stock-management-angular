package com.sgs.backend.mvtStk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la SEULE création manuelle possible depuis l'API : un ajustement
 * d'inventaire. Les mouvements ENTREE/SORTIE ne sont jamais créés
 * directement par un appel de ce DTO -- ils sont générés par
 * MvtStkService.enregistrerMouvement() en interne, depuis
 * CommandeFournisseurService (réception), CommandeClientService (validation)
 * et VenteService (création). Exposer ENTREE/SORTIE ici permettrait de
 * faire bouger le stock sans passer par aucune commande, ce qui casserait
 * toute traçabilité métier.
 */
public record MvtStkRequestDTO(
        @NotNull(message = "l'article est obligatoire")
        Long articleId,

        // Signée : positive pour ajouter, négative pour retirer.
        @NotNull(message = "la quantité est obligatoire")
        Integer quantite,

        @NotBlank(message = "le motif est obligatoire pour un ajustement manuel")
        String motif
) {}
