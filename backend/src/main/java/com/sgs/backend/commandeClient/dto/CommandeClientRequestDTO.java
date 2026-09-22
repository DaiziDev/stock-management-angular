package com.sgs.backend.commandeClient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CommandeClientRequestDTO(
        @NotNull(message = "le client est obligatoire")
        Long clientId,

        @NotEmpty(message = "une commande doit contenir au moins une ligne")
        @Valid
        List<LigneCommandeRequestDTO> lignes
) {}
