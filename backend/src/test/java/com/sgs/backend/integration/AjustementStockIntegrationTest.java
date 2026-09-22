package com.sgs.backend.integration;

import com.sgs.backend.article.Article;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.StockInsuffisantException;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.mvtStk.dto.MvtStkRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RG-03 (toute entrée est un MvtStk tracé), RG-05 (le stock ne peut pas
 * être négatif) et RG-06 (ajustement signé avec motif obligatoire), au
 * niveau du cœur métier : MvtStkService.enregistrerMouvement() — le SEUL
 * endroit du code autorisé à modifier Article.stockActuel.
 */
class AjustementStockIntegrationTest extends AbstractStockIntegrationTest {

    @Autowired
    private MvtStkService mvtStkService;

    @Test
    void entreeDeStockIncrementeEtEstTracee_RG03() {
        Article article = creerArticle("Article entrée", 5, null);
        Article recharge = articleRepository.findById(article.getId()).orElseThrow();

        mvtStkService.enregistrerMouvement(recharge, TypeMouvement.ENTREE, 10, null, "CF-TEST");

        assertThat(stockDe(article.getId())).isEqualTo(15);
        var mouvements = mouvementsDe(article.getId());
        assertThat(mouvements).hasSize(1);
        assertThat(mouvements.get(0).getType()).isEqualTo(TypeMouvement.ENTREE);
        assertThat(mouvements.get(0).getQuantite()).isEqualTo(10);
        assertThat(mouvements.get(0).getStockApresMouvement()).isEqualTo(15);
        assertThat(mouvements.get(0).getOrigine()).isEqualTo("CF-TEST");
    }

    @Test
    void sortieDeStockDecrementeEtEstTracee_RG02_RG04() {
        Article article = creerArticle("Article sortie", 5, null);
        Article charge = articleRepository.findById(article.getId()).orElseThrow();

        mvtStkService.enregistrerMouvement(charge, TypeMouvement.SORTIE, 3, null, "VT-TEST");

        assertThat(stockDe(article.getId())).isEqualTo(2);
        var mouvements = mouvementsDe(article.getId());
        assertThat(mouvements).hasSize(1);
        assertThat(mouvements.get(0).getType()).isEqualTo(TypeMouvement.SORTIE);
        assertThat(mouvements.get(0).getStockApresMouvement()).isEqualTo(2);
    }

    @Test
    void sortieAuDelaDuStockEstRefusee_RG05() {
        Article article = creerArticle("Article sous-stocké", 5, null);
        Article charge = articleRepository.findById(article.getId()).orElseThrow();

        assertThatThrownBy(() ->
                mvtStkService.enregistrerMouvement(charge, TypeMouvement.SORTIE, 6, null, "VT-TEST"))
                .isInstanceOf(StockInsuffisantException.class)
                .hasMessageContaining("Stock insuffisant");

        // RG-05 : ni le stock, ni la trace ne doivent avoir bougé.
        assertThat(stockDe(article.getId())).isEqualTo(5);
        assertThat(mouvementsDe(article.getId())).isEmpty();
    }

    @Test
    void ajustementSigneAvecMotifEstTrace_RG06() {
        Article article = creerArticle("Article à ajuster", 10, null);

        mvtStkService.creerAjustement(new MvtStkRequestDTO(article.getId(), 4, "Inventaire : +4 trouvés"));
        assertThat(stockDe(article.getId())).isEqualTo(14);

        mvtStkService.creerAjustement(new MvtStkRequestDTO(article.getId(), -3, "Casse déclarée"));
        assertThat(stockDe(article.getId())).isEqualTo(11);

        var mouvements = mouvementsDe(article.getId());
        assertThat(mouvements).hasSize(2);
        assertThat(mouvements.get(0).getType()).isEqualTo(TypeMouvement.AJUSTEMENT);
        assertThat(mouvements.get(0).getMotif()).isEqualTo("Casse déclarée");
        assertThat(mouvements.get(1).getMotif()).isEqualTo("Inventaire : +4 trouvés");
    }

    @Test
    void ajustementQuiRendLeStockNegatifEstRefuse_RG05() {
        Article article = creerArticle("Article faible", 2, null);

        assertThatThrownBy(() ->
                mvtStkService.creerAjustement(new MvtStkRequestDTO(article.getId(), -5, "Erreur de saisie")))
                .isInstanceOf(StockInsuffisantException.class);

        assertThat(stockDe(article.getId())).isEqualTo(2);
        assertThat(mouvementsDe(article.getId())).isEmpty();
    }

    @Test
    void ajustementSurArticleDunAutreTenantEstRefuse() {
        Article articleExterne = new Article();
        articleExterne.setCodeArticle("TEST-ART-EXTERNE");
        articleExterne.setDesignation("Article autre entreprise");
        articleExterne.setPrixUnitaireHt(new java.math.BigDecimal("500.00"));
        articleExterne.setTauxTva(new java.math.BigDecimal("19.00"));
        articleExterne.setPrixUnitaireTtc(new java.math.BigDecimal("595.00"));
        articleExterne.setStockActuel(3);
        articleExterne.setEntreprise(autreEntreprise);
        articleExterne = articleRepository.save(articleExterne);
        final Long articleExterneId = articleExterne.getId();

        // L'utilisateur connecté appartient à `entreprise`, pas à `autreEntreprise` :
        // l'article est "introuvable" pour lui (jamais "interdit", RG-10).
        assertThatThrownBy(() ->
                mvtStkService.creerAjustement(new MvtStkRequestDTO(articleExterneId, 1, "Tentative cross-tenant")))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(stockDe(articleExterneId)).isEqualTo(3);
    }
}
