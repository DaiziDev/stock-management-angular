package com.sgs.backend.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// Un DTO de requête ne référence JAMAIS l'entité liée directement (pas de
// "Categorie categorie" ici). On demande juste son id : c'est au Service
// d'aller chercher l'entité Categorie correspondante en base et de vérifier
// qu'elle existe. Le client (frontend) ne manipule que des id, jamais des
// entités JPA.
public record ArticleRequestDTO(
        @NotBlank(message = "le code article est obligatoire")
        String codeArticle,

        @NotBlank(message = "la désignation est obligatoire")
        String designation,

        @NotNull(message = "le prix unitaire HT est obligatoire")
        @PositiveOrZero(message = "le prix unitaire HT ne peut pas être négatif")
        BigDecimal prixUnitaireHt,

        @NotNull(message = "le taux de TVA est obligatoire")
        @PositiveOrZero(message = "le taux de TVA ne peut pas être négatif")
        BigDecimal tauxTva,

        String photo,

        // Pas de stockActuel ici : le stock ne se fixe jamais depuis ce DTO,
        // seulement via un MvtStk (création d'article -> stock à 0, puis
        // modifié par un ajustement ou une réception -- RG-06 du cahier
        // des charges : toute correction de stock exige un motif tracé).
        @PositiveOrZero(message = "le seuil minimum ne peut pas être négatif")
        Integer seuilMin,

        @NotNull(message = "la catégorie est obligatoire")
        Long categorieId
) {}
