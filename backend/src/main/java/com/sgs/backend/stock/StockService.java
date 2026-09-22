package com.sgs.backend.stock;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.stock.dto.ArticleStockDTO;
import com.sgs.backend.stock.dto.ValorisationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lecture seule sur Article -- aucune écriture ici. Pas d'entité propre :
 * "l'état du stock" est une vue dérivée d'Article.stockActuel/seuilMin,
 * pas une donnée qui vit indépendamment.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ArticleRepository articleRepository;
    private final CurrentUserService currentUserService;

    public List<ArticleStockDTO> etat() {
        return articlesDuTenant().stream()
                .map(this::toStockDTO)
                .toList();
    }

    // Un article sans seuilMin configuré n'est jamais en alerte : on ne
    // peut pas comparer à un seuil qui n'existe pas.
    public List<ArticleStockDTO> alertes() {
        return articlesDuTenant().stream()
                .filter(a -> a.getSeuilMin() != null && a.getStockActuel() <= a.getSeuilMin())
                .map(this::toStockDTO)
                .toList();
    }

    public ValorisationResponseDTO valorisation() {
        BigDecimal total = articlesDuTenant().stream()
                .map(a -> a.getPrixUnitaireHt().multiply(BigDecimal.valueOf(a.getStockActuel())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ValorisationResponseDTO(total);
    }

    private List<Article> articlesDuTenant() {
        return articleRepository.findByEntrepriseId(currentUserService.getEntrepriseId());
    }

    private ArticleStockDTO toStockDTO(Article article) {
        return new ArticleStockDTO(
                article.getId(),
                article.getCodeArticle(),
                article.getDesignation(),
                article.getStockActuel(),
                article.getSeuilMin()
        );
    }
}
