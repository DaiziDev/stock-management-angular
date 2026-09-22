package com.sgs.backend.mvtStk.dto;

import com.sgs.backend.mvtStk.TypeMouvement;

import java.time.Instant;

public record MvtStkResponseDTO(
        Long id,
        TypeMouvement type,
        int quantite,
        Instant dateMouvement,
        String motif,
        String origine,
        Long articleId,
        String articleDesignation,
        int stockActuelApres
) {}
