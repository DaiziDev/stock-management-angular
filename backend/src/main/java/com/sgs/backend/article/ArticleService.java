package com.sgs.backend.article;

import com.sgs.backend.article.dto.ArticleRequestDTO;
import com.sgs.backend.article.dto.ArticleResponseDTO;
import com.sgs.backend.categorie.Categorie;
import com.sgs.backend.categorie.CategorieRepository;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    // On injecte aussi CategorieRepository ici : c'est ce Service qui a besoin
    // d'aller vérifier que la catégorie envoyée par id existe réellement.
    // Ce n'est PAS le rôle du Controller (routage HTTP) ni celui du
    // Repository Article (accès aux données Article uniquement).
    private final CategorieRepository categorieRepository;
    private final CurrentUserService currentUserService;

    public List<ArticleResponseDTO> findAll() {
        return articleRepository.findByEntrepriseId(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ArticleResponseDTO findById(Long id) {
        Article article = getArticleOrThrow(id);
        return toResponseDTO(article);
    }

    public ArticleResponseDTO create(ArticleRequestDTO dto) {
        if (articleRepository.existsByCodeArticle(dto.codeArticle())) {
            throw new IllegalArgumentException("Un article avec le code '" + dto.codeArticle() + "' existe déjà");
        }
        Categorie categorie = getCategorieOrThrow(dto.categorieId());

        Article article = new Article();
        applyDto(article, dto, categorie);
        // L'entreprise n'est jamais reçue du client : elle est toujours déduite
        // du JWT de l'utilisateur connecté (voir CurrentUserService).
        article.setEntreprise(currentUserService.getEntrepriseCourante());

        Article saved = articleRepository.save(article);
        return toResponseDTO(saved);
    }

    public ArticleResponseDTO update(Long id, ArticleRequestDTO dto) {
        Article article = getArticleOrThrow(id);
        Categorie categorie = getCategorieOrThrow(dto.categorieId());

        applyDto(article, dto, categorie);

        Article saved = articleRepository.save(article);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        Article article = getArticleOrThrow(id);
        articleRepository.delete(article);
    }

    // --- Helpers privés ---

    // Ne renvoie l'article que s'il appartient à l'entreprise de l'utilisateur
    // connecté. Un article d'une autre entreprise est traité comme
    // "introuvable" (404), jamais comme "interdit" (403) -- pour ne pas
    // révéler à un tenant que l'id existe chez un autre (RG-10).
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

    private Categorie getCategorieOrThrow(Long categorieId) {
        return categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec id=" + categorieId));
    }

    private void applyDto(Article article, ArticleRequestDTO dto, Categorie categorie) {
        article.setCodeArticle(dto.codeArticle());
        article.setDesignation(dto.designation());
        article.setPrixUnitaireHt(dto.prixUnitaireHt());
        article.setTauxTva(dto.tauxTva());
        // Le TTC n'est jamais reçu du client : on le calcule nous-mêmes.
        // Règle : c'est une info dérivée, pas une info saisie -- si on
        // laissait le frontend l'envoyer, rien n'empêcherait un TTC
        // incohérent avec le HT et la TVA envoyés à côté.
        article.setPrixUnitaireTtc(calculerTtc(dto.prixUnitaireHt(), dto.tauxTva()));
        article.setPhoto(dto.photo());
        article.setSeuilMin(dto.seuilMin());
        article.setCategorie(categorie);
    }

    private BigDecimal calculerTtc(BigDecimal prixHt, BigDecimal tauxTva) {
        // TTC = HT * (1 + taux/100), taux exprimé en pourcentage (ex: 19.25)
        BigDecimal coefficient = BigDecimal.ONE.add(tauxTva.divide(BigDecimal.valueOf(100)));
        return prixHt.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private ArticleResponseDTO toResponseDTO(Article article) {
        ArticleResponseDTO.CategorieSummaryDTO categorieDto = article.getCategorie() != null
                ? new ArticleResponseDTO.CategorieSummaryDTO(
                        article.getCategorie().getId(),
                        article.getCategorie().getDesignation())
                : null;

        return new ArticleResponseDTO(
                article.getId(),
                article.getCodeArticle(),
                article.getDesignation(),
                article.getPrixUnitaireHt(),
                article.getTauxTva(),
                article.getPrixUnitaireTtc(),
                article.getPhoto(),
                article.getStockActuel(),
                article.getSeuilMin(),
                categorieDto
        );
    }
}
