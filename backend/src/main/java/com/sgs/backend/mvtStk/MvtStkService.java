package com.sgs.backend.mvtStk;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.mvtStk.dto.MvtStkRequestDTO;
import com.sgs.backend.mvtStk.dto.MvtStkResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cœur du module Stock : c'est le SEUL endroit du code qui a le droit de
 * modifier Article.stockActuel. CommandeFournisseurService,
 * CommandeClientService et VenteService appellent enregistrerMouvement() au
 * lieu de toucher directement au champ -- ça garantit qu'aucun mouvement de
 * stock n'existe sans un MvtStk qui le trace (RG-02/RG-03/RG-04).
 */
@Service
@RequiredArgsConstructor
public class MvtStkService {

    private final MvtStkRepository mvtStkRepository;
    private final ArticleRepository articleRepository;
    private final CurrentUserService currentUserService;

    /**
     * Applique un mouvement à l'article et enregistre sa trace, dans la même
     * transaction que l'appelant (CommandeFournisseurService.receptionner,
     * CommandeClientService.valider, VenteService.create...). Si ça lève
     * StockInsuffisantException au milieu d'une boucle sur plusieurs lignes,
     * Spring annule TOUTE la transaction : aucune ligne déjà traitée ne
     * reste appliquée. C'est ce qui implémente "vente refusée en bloc".
     */
    @Transactional
    public MvtStk enregistrerMouvement(Article article, TypeMouvement type, int quantite, String motif, String origine) {
        int nouveauStock = switch (type) {
            case ENTREE -> article.getStockActuel() + quantite;
            case SORTIE -> article.getStockActuel() - quantite;
            case AJUSTEMENT -> article.getStockActuel() + quantite; // quantite déjà signée
        };

        if (nouveauStock < 0) {
            throw new StockInsuffisantException(
                    "Stock insuffisant pour l'article '" + article.getDesignation() + "' "
                            + "(disponible : " + article.getStockActuel() + ", demandé : " + quantite + ")"
            );
        }

        article.setStockActuel(nouveauStock);
        articleRepository.save(article);

        MvtStk mouvement = new MvtStk();
        mouvement.setType(type);
        mouvement.setQuantite(quantite);
        mouvement.setDateMouvement(Instant.now());
        mouvement.setMotif(motif);
        mouvement.setOrigine(origine);
        mouvement.setStockApresMouvement(nouveauStock);
        mouvement.setArticle(article);
        mouvement.setEntreprise(article.getEntreprise());
        return mvtStkRepository.save(mouvement);
    }

    /**
     * Seul point d'entrée public pour créer un mouvement "à la main" (depuis
     * le controller) : toujours un AJUSTEMENT, jamais ENTREE/SORTIE (voir
     * commentaire sur MvtStkRequestDTO).
     */
    public MvtStkResponseDTO creerAjustement(MvtStkRequestDTO dto) {
        Article article = getArticleOrThrow(dto.articleId());
        MvtStk mouvement = enregistrerMouvement(article, TypeMouvement.AJUSTEMENT, dto.quantite(), dto.motif(), "Ajustement manuel");
        return toResponseDTO(mouvement);
    }

    /**
     * Historique des mouvements, filtrable par article et/ou type. Le
     * filtrage est poussé en SQL (§5.1) : on ne charge plus tout
     * l'historique de l'entreprise en mémoire pour le filtrer en Java, mais
     * seulement les lignes qui matchent les critères.
     */
    public List<MvtStkResponseDTO> findAll(Long articleId, TypeMouvement type) {
        Long entrepriseId = currentUserService.getEntrepriseId();

        if (articleId != null && type != null) {
            return mvtStkRepository
                    .findByEntrepriseIdAndArticleIdAndTypeOrderByDateMouvementDesc(entrepriseId, articleId, type)
                    .stream().map(this::toResponseDTO).toList();
        }
        if (articleId != null) {
            return mvtStkRepository
                    .findByEntrepriseIdAndArticleIdOrderByDateMouvementDesc(entrepriseId, articleId)
                    .stream().map(this::toResponseDTO).toList();
        }
        if (type != null) {
            return mvtStkRepository
                    .findByEntrepriseIdAndTypeOrderByDateMouvementDesc(entrepriseId, type)
                    .stream().map(this::toResponseDTO).toList();
        }
        return mvtStkRepository.findByEntrepriseIdOrderByDateMouvementDesc(entrepriseId)
                .stream().map(this::toResponseDTO).toList();
    }

    // Même logique de tenant que les autres Services (voir ArticleService) :
    // un article d'une autre entreprise est "introuvable", pas "interdit".
    private Article getArticleOrThrow(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = article.getEntreprise() != null
                && article.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Article introuvable avec id=" + id);
        }
        return article;
    }

    private MvtStkResponseDTO toResponseDTO(MvtStk mouvement) {
        return new MvtStkResponseDTO(
                mouvement.getId(),
                mouvement.getType(),
                mouvement.getQuantite(),
                mouvement.getDateMouvement(),
                mouvement.getMotif(),
                mouvement.getOrigine(),
                mouvement.getArticle().getId(),
                mouvement.getArticle().getDesignation(),
                mouvement.getStockApresMouvement()
        );
    }
}
