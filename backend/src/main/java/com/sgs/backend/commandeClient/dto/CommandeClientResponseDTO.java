package com.sgs.backend.commandeClient.dto;

import com.sgs.backend.commande.StatutCommandeClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CommandeClientResponseDTO(
        Long id,
        String code,
        Instant dateCommande,
        StatutCommandeClient statut,
        Long clientId,
        String clientNom,
        List<LigneCommandeResponseDTO> lignes,
        BigDecimal total
) {}
