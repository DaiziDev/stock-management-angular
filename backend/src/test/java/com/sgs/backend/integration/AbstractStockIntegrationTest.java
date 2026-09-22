package com.sgs.backend.integration;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.client.Client;
import com.sgs.backend.client.ClientRepository;
import com.sgs.backend.commandeClient.CommandeClientRepository;
import com.sgs.backend.commandeFournisseur.CommandeFournisseurRepository;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.fournisseur.Fournisseur;
import com.sgs.backend.fournisseur.FournisseurRepository;
import com.sgs.backend.mvtStk.MvtStk;
import com.sgs.backend.mvtStk.MvtStkRepository;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import com.sgs.backend.vente.VenteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Socle des tests d'intégration des règles de stock (RG-02 à RG-06).
 *
 * Stratégie d'isolation :
 *   * la transaction de test est annulée à la fin de CHAQUE test
 *     (@Transactional par défaut en rollback) : aucune donnée de test
 *     n'est persistée ;
 *   * par sécurité (et pour les cas où un service ouvrirait sa propre
 *     transaction), chaque test ne crée QUE des données préfixées
 *     "TEST-", rattachées à des entreprises "TEST-Ent-<uuid>" créées à
 *     la volée, et les supprime avant/après chaque test — les données
 *     de développement ne sont jamais touchées ;
 *   * le profil "test" redirige le SMTP vers localhost : les emails
 *     échouent vite et sont absorbés par EmailService (best-effort).
 *
 * L'authentification est simulée en posant un Authentication dans le
 * SecurityContext (comme le ferait JwtAuthFilter) : CurrentUserService
 * déduit l'entreprise du login, exactement comme en production.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AbstractStockIntegrationTest {

    /** Préfixe exclusif des données créées par les tests. */
    protected static final String PREFIXE_TEST = "TEST-";

    @Autowired protected EntrepriseRepository entrepriseRepository;
    @Autowired protected UtilisateurRepository utilisateurRepository;
    @Autowired protected ArticleRepository articleRepository;
    @Autowired protected ClientRepository clientRepository;
    @Autowired protected FournisseurRepository fournisseurRepository;
    @Autowired protected CommandeClientRepository commandeClientRepository;
    @Autowired protected CommandeFournisseurRepository commandeFournisseurRepository;
    @Autowired protected VenteRepository venteRepository;
    @Autowired protected MvtStkRepository mvtStkRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected CurrentUserService currentUserService;

    @Autowired private JdbcTemplate jdbcTemplate;

    /** MockMvc avec la vraie chaîne de filtres Spring Security. */
    @Autowired protected MockMvc mvc;

    /** Entreprise "tenant" du test courant. */
    protected Entreprise entreprise;
    /** Admin de {@link #entreprise} — l'utilisateur connecté par défaut. */
    protected Utilisateur utilisateur;
    /** Second tenant, pour les tests d'isolation multi-entreprise. */
    protected Entreprise autreEntreprise;
    /** Admin de {@link #autreEntreprise}. */
    protected Utilisateur autreUtilisateur;

    @BeforeEach
    void preparerEnvironnement() {
        supprimerDonneesDeTest();
        entreprise = creerEntreprise("TEST-Ent-" + UUID.randomUUID());
        autreEntreprise = creerEntreprise("TEST-Ent-" + UUID.randomUUID());
        utilisateur = creerUtilisateur(entreprise);
        autreUtilisateur = creerUtilisateur(autreEntreprise);
        simulerAuthentification(utilisateur);
    }

    @AfterEach
    void nettoyerEnvironnement() {
        SecurityContextHolder.clearContext();
        supprimerDonneesDeTest();
    }

    // ───────────── Authentification simulée ─────────────

    /** Pose l'Authentication que JwtAuthFilter poserait après validation du JWT. */
    protected void simulerAuthentification(Utilisateur u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getLogin(), "n/a", List.of())
        );
    }

    // ───────────── Fixtures ─────────────

    protected Entreprise creerEntreprise(String nom) {
        Entreprise e = new Entreprise();
        e.setNom(nom);
        e.setMail("contact@test.local");
        return entrepriseRepository.save(e);
    }

    protected Utilisateur creerUtilisateur(Entreprise e) {
        Utilisateur u = new Utilisateur();
        u.setNom("Test");
        u.setPrenom("Admin");
        u.setLogin(PREFIXE_TEST + UUID.randomUUID() + "@sgs.test");
        u.setMotDePasse(passwordEncoder.encode("MotDePasse123!"));
        u.setRole(UserRole.ADMIN);
        u.setEntreprise(e);
        return utilisateurRepository.save(u);
    }

    protected Article creerArticle(String designation, int stock, Integer seuilMin) {
        Article a = new Article();
        a.setCodeArticle(PREFIXE_TEST + "ART-" + UUID.randomUUID());
        a.setDesignation(designation);
        a.setPrixUnitaireHt(new BigDecimal("1000.00"));
        a.setTauxTva(new BigDecimal("19.00"));
        a.setPrixUnitaireTtc(new BigDecimal("1190.00"));
        a.setStockActuel(stock);
        a.setSeuilMin(seuilMin);
        a.setEntreprise(entreprise);
        return articleRepository.save(a);
    }

    protected Client creerClient() {
        Client c = new Client();
        c.setNom("Nkoulou");
        c.setPrenom("Jean");
        c.setMail("client@test.local");
        c.setEntreprise(entreprise);
        return clientRepository.save(c);
    }

    protected Fournisseur creerFournisseur() {
        Fournisseur f = new Fournisseur();
        f.setNom(PREFIXE_TEST + "Fournisseur " + UUID.randomUUID());
        f.setEntreprise(entreprise);
        return fournisseurRepository.save(f);
    }

    // ───────────── Lectures d'état ─────────────

    /**
     * Le stock est relu en SQL brut : il n'y a PAS de transaction de test
     * (volontaire — le rollback en bloc doit se produire dans les transactions
     * métier), donc chaque lecture fraîche passe par JdbcTemplate, hors du
     * cache Hibernate et de tout contexte de persistance périmé.
     */
    protected int stockDe(Long articleId) {
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stockactuel FROM article WHERE id = ?", Integer.class, articleId);
        return stock != null ? stock : 0;
    }

    /** Mouvements de l'article pour le tenant courant, rechargés depuis la base. */
    protected List<MvtStk> mouvementsDe(Long articleId) {
        return mvtStkRepository.findByEntrepriseIdOrderByDateMouvementDesc(entreprise.getId()).stream()
                .filter(m -> articleId.equals(m.getArticle().getId()))
                .toList();
    }

    // ───────────── Nettoyage ciblé ─────────────

    /**
     * Supprime TOUTES les données des entreprises "TEST-*" (enfants avant
     * parents, contraintes FK oblige). Ne touche jamais aux autres données :
     * seules les entreprises préfixées TEST- sont supprimées.
     */
    private void supprimerDonneesDeTest() {
        List<Entreprise> entreprisesDeTest = entrepriseRepository.findAll().stream()
                .filter(e -> e.getNom() != null && e.getNom().startsWith(PREFIXE_TEST))
                .toList();
        for (Entreprise e : entreprisesDeTest) {
            Long id = e.getId();
            jdbcTemplate.update("DELETE FROM mvtstk WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM lignevente WHERE idvente IN (SELECT id FROM vente WHERE identreprise = ?)", id);
            jdbcTemplate.update("DELETE FROM lignecommandeclient WHERE idcommandeclient IN (SELECT id FROM commandeclient WHERE identreprise = ?)", id);
            jdbcTemplate.update("DELETE FROM lignecommandefournisseur WHERE idcommandefournisseur IN (SELECT id FROM commandefournisseur WHERE identreprise = ?)", id);
            jdbcTemplate.update("DELETE FROM vente WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM commandeclient WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM commandefournisseur WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM article WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM client WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM fournisseur WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM categorie WHERE identreprise = ?", id);
            // Les refresh tokens référencent l'utilisateur : enfants d'abord (V5).
            jdbcTemplate.update("DELETE FROM refreshtoken WHERE idutilisateur IN (SELECT id FROM utilisateur WHERE identreprise = ?)", id);
            jdbcTemplate.update("DELETE FROM utilisateur WHERE identreprise = ?", id);
            jdbcTemplate.update("DELETE FROM entreprise WHERE id = ?", id);
        }
    }
}
