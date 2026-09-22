package com.sgs.backend.commandeClient;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.client.Client;
import com.sgs.backend.client.ClientRepository;
import com.sgs.backend.commande.StatutCommandeClient;
import com.sgs.backend.commandeClient.dto.CommandeClientRequestDTO;
import com.sgs.backend.commandeClient.dto.CommandeClientResponseDTO;
import com.sgs.backend.commandeClient.dto.LigneCommandeRequestDTO;
import com.sgs.backend.commandeClient.dto.LigneCommandeResponseDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.ligneCommandeClient.LigneCommandeClient;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandeClientService {

    private final CommandeClientRepository commandeClientRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final MvtStkService mvtStkService;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;

    public List<CommandeClientResponseDTO> findAll() {
        return commandeClientRepository.findByEntrepriseIdOrderByDateCommandeDesc(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CommandeClientResponseDTO findById(Long id) {
        return toResponseDTO(getCommandeOrThrow(id));
    }

    /**
     * Crée la commande ET ses lignes en une seule transaction (roadmap #7) :
     * soit tout est enregistré, soit rien -- pas de commande à moitié
     * remplie si un article de la liste n'existe pas.
     */
    @Transactional
    public CommandeClientResponseDTO create(CommandeClientRequestDTO dto) {
        Client client = getClientOrThrow(dto.clientId());

        CommandeClient commande = new CommandeClient();
        commande.setDateCommande(Instant.now());
        commande.setStatut(StatutCommandeClient.EN_COURS);
        commande.setClient(client);
        commande.setEntreprise(currentUserService.getEntrepriseCourante());
        commande.setLignes(construireLignes(dto.lignes(), commande));

        // Sauvegarde en deux temps : il faut l'id généré pour construire un
        // code lisible ("CC-000042"). Un code séquentiel indépendant de l'id
        // demanderait un compteur dédié -- pas nécessaire pour ce volume.
        commande.setCode("CC-TMP");
        CommandeClient saved = commandeClientRepository.save(commande);
        saved.setCode(String.format("CC-%06d", saved.getId()));
        saved = commandeClientRepository.save(saved);

        // RG-08 : confirmation envoyée au client. Best-effort (voir EmailService) --
        // ne bloque jamais la création de la commande si l'envoi échoue.
        emailService.envoyerConfirmationCommandeClient(
                client.getMail(), saved.getCode(), client.getPrenom() + " " + client.getNom()
        );

        return toResponseDTO(saved);
    }

    /**
     * EN_COURS -> VALIDEE. Génère une sortie de stock par ligne (RG-02).
     * Si une seule ligne manque de stock, MvtStkService lève
     * StockInsuffisantException et @Transactional annule tout : aucune
     * ligne n'est partiellement décrémentée.
     */
    @Transactional
    public CommandeClientResponseDTO valider(Long id) {
        CommandeClient commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeClient.EN_COURS) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_COURS peut être validée (statut actuel : " + commande.getStatut() + ")"
            );
        }

        for (LigneCommandeClient ligne : commande.getLignes()) {
            mvtStkService.enregistrerMouvement(
                    ligne.getArticle(), TypeMouvement.SORTIE, ligne.getQuantite(), null, commande.getCode()
            );
        }

        commande.setStatut(StatutCommandeClient.VALIDEE);
        return toResponseDTO(commandeClientRepository.save(commande));
    }

    /**
     * VALIDEE -> EXPEDIEE (§3.4). La marchandise est remise au transporteur :
     * aucun impact stock, les sorties ont déjà été générées à la validation.
     */
    @Transactional
    public CommandeClientResponseDTO expedier(Long id) {
        CommandeClient commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeClient.VALIDEE) {
            throw new IllegalArgumentException(
                    "Seule une commande VALIDEE peut être expédiée (statut actuel : " + commande.getStatut() + ")"
            );
        }
        commande.setStatut(StatutCommandeClient.EXPEDIEE);
        return toResponseDTO(commandeClientRepository.save(commande));
    }

    /**
     * EXPEDIEE -> LIVREE (§3.4). Livraison confirmée chez le client : aucun
     * impact stock. Il faut être EXPEDIEE pour garder une trace de l'expédition,
     * mais on tolère le raccourci VALIDEE -> LIVREE (livraison directe au
     * comptoir / par vos soins, sans passage par le statut EXPEDIEE).
     */
    @Transactional
    public CommandeClientResponseDTO livrer(Long id) {
        CommandeClient commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeClient.EXPEDIEE
                && commande.getStatut() != StatutCommandeClient.VALIDEE) {
            throw new IllegalArgumentException(
                    "Seule une commande VALIDEE ou EXPEDIEE peut être livrée (statut actuel : " + commande.getStatut() + ")"
            );
        }
        commande.setStatut(StatutCommandeClient.LIVREE);
        return toResponseDTO(commandeClientRepository.save(commande));
    }

    /**
     * EN_COURS -> ANNULEE uniquement : une commande déjà VALIDEE a déjà
     * généré ses sorties de stock, l'annuler nécessiterait un mouvement
     * inverse (un "avoir"), hors périmètre pour l'instant.
     */
    public CommandeClientResponseDTO annuler(Long id) {
        CommandeClient commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeClient.EN_COURS) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_COURS peut être annulée (statut actuel : " + commande.getStatut() + ")"
            );
        }
        commande.setStatut(StatutCommandeClient.ANNULEE);
        return toResponseDTO(commandeClientRepository.save(commande));
    }

    // --- Helpers privés ---

    private List<LigneCommandeClient> construireLignes(List<LigneCommandeRequestDTO> lignesDto, CommandeClient commande) {
        List<LigneCommandeClient> lignes = new ArrayList<>();
        for (LigneCommandeRequestDTO ligneDto : lignesDto) {
            Article article = getArticleOrThrow(ligneDto.articleId());
            LigneCommandeClient ligne = new LigneCommandeClient();
            ligne.setArticle(article);
            ligne.setQuantite(ligneDto.quantite());
            // Copié maintenant, jamais recalculé si le prix de l'article change plus tard.
            ligne.setPrixUnitaire(article.getPrixUnitaireTtc());
            ligne.setCommandeClient(commande);
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

    private CommandeClient getCommandeOrThrow(Long id) {
        CommandeClient commande = commandeClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande client introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = commande.getEntreprise() != null
                && commande.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Commande client introuvable avec id=" + id);
        }
        return commande;
    }

    private CommandeClientResponseDTO toResponseDTO(CommandeClient commande) {
        List<LigneCommandeResponseDTO> lignes = commande.getLignes().stream()
                .map(l -> new LigneCommandeResponseDTO(
                        l.getId(),
                        l.getArticle().getId(),
                        l.getArticle().getDesignation(),
                        l.getQuantite(),
                        l.getPrixUnitaire(),
                        l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite()))
                ))
                .toList();

        BigDecimal total = lignes.stream()
                .map(LigneCommandeResponseDTO::sousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CommandeClientResponseDTO(
                commande.getId(),
                commande.getCode(),
                commande.getDateCommande(),
                commande.getStatut(),
                commande.getClient().getId(),
                commande.getClient().getPrenom() + " " + commande.getClient().getNom(),
                lignes,
                total
        );
    }
}
