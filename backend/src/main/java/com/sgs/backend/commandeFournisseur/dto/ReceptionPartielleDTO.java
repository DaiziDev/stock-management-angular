package com.sgs.backend.commandeFournisseur.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corps de PUT /api/commandes-fournisseur/{id}/receptionner-partiel (§3.5).
 * Chaque entrée donne la quantité effectivement reçue pour une ligne de la
 * commande ; les lignes absentes de la liste ne reçoivent rien.
 */
public record ReceptionPartielleDTO(

        @NotNull(message = "La liste des quantités reçues est obligatoire")
        List<LigneRecueDTO> lignes

) {
    public record LigneRecueDTO(

            @NotNull(message = "L'id de la ligne est obligatoire")
            Long ligneId,

            @Min(value = 0, message = "La quantité reçue ne peut pas être négative")
            int quantiteRecue

    ) {}
}
