package com.sgs.backend.stock.dto;

public record ArticleStockDTO(
        Long articleId,
        String codeArticle,
        String designation,
        int stockActuel,
        Integer seuilMin
) {}
