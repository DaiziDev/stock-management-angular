package com.sgs.backend.commandeFournisseur.dto;

import com.sgs.backend.commande.StatutCommandeFournisseur;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CommandeFournisseurResponseDTO(
        Long id,
        String code,
        Instant dateCommande,
        StatutCommandeFournisseur statut,
        Long fournisseurId,
        String fournisseurNom,
        List<LigneCommandeFournisseurResponseDTO> lignes,
        BigDecimal total
) {}
