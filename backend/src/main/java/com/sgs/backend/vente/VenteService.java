package com.sgs.backend.vente;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.client.Client;
import com.sgs.backend.client.ClientRepository;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.ligneVente.LigneVente;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.vente.dto.LigneVenteRequestDTO;
import com.sgs.backend.vente.dto.LigneVenteResponseDTO;
import com.sgs.backend.vente.dto.VenteRequestDTO;
import com.sgs.backend.vente.dto.VenteResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cœur du métier (roadmap #10) : pas d'étape "valider" séparée comme pour
 * CommandeClient -- une vente décrémente le stock immédiatement à la
 * création. Pas de update/delete non plus : une vente enregistrée est un
 * fait historique, on ne réécrit pas l'historique des ventes.
 */
@Service
@RequiredArgsConstructor
public class VenteService {

    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final MvtStkService mvtStkService;
    private final CurrentUserService currentUserService;

    public List<VenteResponseDTO> findAll() {
        return venteRepository.findByEntrepriseIdOrderByDateVenteDesc(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public VenteResponseDTO findById(Long id) {
        return toResponseDTO(getVenteOrThrow(id));
    }

    /**
     * Si une seule ligne manque de stock, MvtStkService lève
     * StockInsuffisantException et @Transactional annule TOUT : la vente
     * elle-même et les lignes déjà décrémentées avant l'échec. C'est cette
     * annulation transactionnelle qui implémente "vente refusée en bloc,
     * pas de vente partielle" (règle la plus stricte du cahier des charges).
     */
    @Transactional
    public VenteResponseDTO create(VenteRequestDTO dto) {
        Client client = dto.clientId() != null ? getClientOrThrow(dto.clientId()) : null;

        Vente vente = new Vente();
        vente.setDateVente(Instant.now());
        vente.setClient(client);
        vente.setEntreprise(currentUserService.getEntrepriseCourante());
        vente.setLignes(construireLignes(dto.lignes(), vente));

        vente.setCode("VT-TMP");
        Vente saved = venteRepository.save(vente);
        saved.setCode(String.format("VT-%06d", saved.getId()));
        saved = venteRepository.save(saved);

        for (LigneVente ligne : saved.getLignes()) {
            mvtStkService.enregistrerMouvement(
                    ligne.getArticle(), TypeMouvement.SORTIE, ligne.getQuantite(), null, saved.getCode()
            );
        }

        return toResponseDTO(saved);
    }

    // --- Helpers privés ---

    private List<LigneVente> construireLignes(List<LigneVenteRequestDTO> lignesDto, Vente vente) {
        List<LigneVente> lignes = new ArrayList<>();
        for (LigneVenteRequestDTO ligneDto : lignesDto) {
            Article article = getArticleOrThrow(ligneDto.articleId());
            LigneVente ligne = new LigneVente();
            ligne.setArticle(article);
            ligne.setQuantite(ligneDto.quantite());
            ligne.setPrixUnitaire(article.getPrixUnitaireTtc());
            ligne.setVente(vente);
            lignes.add(ligne);
        }
        return lignes;
    }

    private Client getClientOrThrow(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = client.getEntreprise() != null
                && client.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Client introuvable avec id=" + id);
        }
        return client;
    }

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

    private Vente getVenteOrThrow(Long id) {
        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = vente.getEntreprise() != null
                && vente.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Vente introuvable avec id=" + id);
        }
        return vente;
    }

    private VenteResponseDTO toResponseDTO(Vente vente) {
        List<LigneVenteResponseDTO> lignes = vente.getLignes().stream()
                .map(l -> new LigneVenteResponseDTO(
                        l.getId(),
                        l.getArticle().getId(),
                        l.getArticle().getDesignation(),
                        l.getQuantite(),
                        l.getPrixUnitaire(),
                        l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite()))
                ))
                .toList();

        BigDecimal total = lignes.stream()
                .map(LigneVenteResponseDTO::sousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new VenteResponseDTO(
                vente.getId(),
                vente.getCode(),
                vente.getDateVente(),
                vente.getClient() != null ? vente.getClient().getId() : null,
                vente.getClient() != null ? vente.getClient().getPrenom() + " " + vente.getClient().getNom() : "Client comptoir",
                lignes,
                total
        );
    }
}
