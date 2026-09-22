package com.sgs.backend.integration;

import com.sgs.backend.article.Article;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.mvtStk.dto.MvtStkRequestDTO;
import com.sgs.backend.utilisateur.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règles de stock vues depuis l'API HTTP (chaîne de filtres Spring Security
 * + validation des DTOs + GlobalExceptionHandler) :
 *   * RG-06 : l'ajustement manuel exige un motif (@NotBlank -> 400) ;
 *   * RG-05 : un ajustement qui rendrait le stock négatif -> 409 Conflict
 *     (jamais 500), stock inchangé derrière ;
 *   * isolation : l'historique des mouvements est filtré par entreprise.
 */
class ReglesStockApiIntegrationTest extends AbstractStockIntegrationTest {

    @Autowired
    private MvtStkService mvtStkService;

    /** Authentification MockMvc identique à ce que JwtAuthFilter poserait. */
    private static RequestPostProcessor auth(Utilisateur u) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(u.getLogin(), "n/a", List.of())
        );
    }

    @Test
    void ajustementSansAuthentificationEstRefuse() throws Exception {
        Article article = creerArticle("Article API", 10, null);

        // Le SecurityContext posé par le @BeforeEach fuiterait dans MockMvc
        // (même thread) : on le vide explicitement pour tester SANS auth.
        SecurityContextHolder.clearContext();

        mvc.perform(post("/api/mouvements-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\": %d, \"quantite\": 1, \"motif\": \"Sans auth\"}".formatted(article.getId())))
                .andExpect(status().isForbidden());

        // Sans authentification, rien ne doit avoir été appliqué.
        assertThat(stockDe(article.getId())).isEqualTo(10);
    }

    @Test
    void ajustementSansMotifEstRefuse400_RG06() throws Exception {
        Article article = creerArticle("Article sans motif", 10, null);

        mvc.perform(post("/api/mouvements-stock")
                        .with(auth(utilisateur))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\": %d, \"quantite\": 1}".formatted(article.getId())))
                .andExpect(status().isBadRequest());

        assertThat(stockDe(article.getId())).isEqualTo(10);
    }

    @Test
    void ajustementValideRenvoie201EtUneTrace_RG06() throws Exception {
        Article article = creerArticle("Article ajusté", 10, null);

        mvc.perform(post("/api/mouvements-stock")
                        .with(auth(utilisateur))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\": %d, \"quantite\": 2, \"motif\": \"Inventaire physique\"}".formatted(article.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("AJUSTEMENT"))
                .andExpect(jsonPath("$.motif").value("Inventaire physique"))
                .andExpect(jsonPath("$.stockActuelApres").value(12))
                .andExpect(jsonPath("$.articleId").value(article.getId()));

        assertThat(stockDe(article.getId())).isEqualTo(12);
    }

    @Test
    void ajustementQuiRendLeStockNegatifRenvoie409_RG05() throws Exception {
        Article article = creerArticle("Article 409", 2, null);

        mvc.perform(post("/api/mouvements-stock")
                        .with(auth(utilisateur))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\": %d, \"quantite\": -50, \"motif\": \"Retrait excessif\"}".formatted(article.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Stock insuffisant")));

        assertThat(stockDe(article.getId())).isEqualTo(2);
        assertThat(mouvementsDe(article.getId())).isEmpty();
    }

    @Test
    void historiqueDesMouvementsFiltreParTenant() throws Exception {
        Article article = creerArticle("Article historique", 7, null);
        mvtStkService.creerAjustement(new MvtStkRequestDTO(article.getId(), 1, "Mouvement tenant A"));

        // Le tenant A voit son mouvement.
        mvc.perform(get("/api/mouvements-stock").with(auth(utilisateur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value(TypeMouvement.AJUSTEMENT.name()))
                .andExpect(jsonPath("$[0].origine").value("Ajustement manuel"));

        // Le tenant B ne voit RIEN des mouvements de A.
        mvc.perform(get("/api/mouvements-stock").with(auth(autreUtilisateur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
