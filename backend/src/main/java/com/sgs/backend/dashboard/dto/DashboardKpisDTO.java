package com.sgs.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardKpisDTO(
        BigDecimal valeurStock,
        int nbArticlesEnAlerte,
        long nbCommandesClientEnCours,
        long nbCommandesFournisseurEnAttente,
        long nbVentesDuMois,
        BigDecimal chiffreAffairesDuMois
) {}
