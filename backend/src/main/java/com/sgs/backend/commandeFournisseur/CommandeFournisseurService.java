package com.sgs.backend.commandeFournisseur;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.commande.StatutCommandeFournisseur;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurResponseDTO;
import com.sgs.backend.commandeFournisseur.dto.LigneCommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.LigneCommandeFournisseurResponseDTO;
import com.sgs.backend.commandeFournisseur.dto.ReceptionPartielleDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.fournisseur.Fournisseur;
import com.sgs.backend.fournisseur.FournisseurRepository;
import com.sgs.backend.ligneCommandeFournisseur.LigneCommandeFournisseur;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommandeFournisseurService {

    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ArticleRepository articleRepository;
    private final MvtStkService mvtStkService;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;

    public List<CommandeFournisseurResponseDTO> findAll() {
        return commandeFournisseurRepository.findByEntrepriseIdOrderByDateCommandeDesc(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CommandeFournisseurResponseDTO findById(Long id) {
        return toResponseDTO(getCommandeOrThrow(id));
    }

    @Transactional
    public CommandeFournisseurResponseDTO create(CommandeFournisseurRequestDTO dto) {
        Fournisseur fournisseur = getFournisseurOrThrow(dto.fournisseurId());

        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(Instant.now());
        commande.setStatut(StatutCommandeFournisseur.EN_ATTENTE);
        commande.setFournisseur(fournisseur);
        commande.setEntreprise(currentUserService.getEntrepriseCourante());
        commande.setLignes(construireLignes(dto.lignes(), commande));

        commande.setCode("CF-TMP");
        CommandeFournisseur saved = commandeFournisseurRepository.save(commande);
        saved.setCode(String.format("CF-%06d", saved.getId()));
        saved = commandeFournisseurRepository.save(saved);

        // RG-07 : bon de commande envoyé au fournisseur. Best-effort (voir EmailService).
        emailService.envoyerBonCommandeFournisseur(fournisseur.getMail(), saved.getCode(), fournisseur.getNom());

        return toResponseDTO(saved);
    }

    /**
     * -> RECUE (réception complète). Génère une entrée de stock par ligne (RG-03).
     * Accepté depuis EN_ATTENTE comme depuis RECUE_PARTIELLEMENT (§3.5) : la
     * réception finale du solde d'une commande partiellement reçue est une
     * réception complète. Une commande déjà RECUE est refusée -- sinon le
     * stock serait compté deux fois (règle explicite du flux fonctionnel).
     */
    @Transactional
    public CommandeFournisseurResponseDTO receptionner(Long id) {
        CommandeFournisseur commande = getCommandeOrThrow(id);
        if (commande.getStatut() == StatutCommandeFournisseur.RECUE) {
            throw new IllegalArgumentException(
                    "Cette commande est déjà reçue intégralement (le stock serait compté deux fois)"
            );
        }
        if (commande.getStatut() == StatutCommandeFournisseur.ANNULEE) {
            throw new IllegalArgumentException(
                    "Une commande annulée ne peut pas être réceptionnée (statut actuel : " + commande.getStatut() + ")"
            );
        }

        for (LigneCommandeFournisseur ligne : commande.getLignes()) {
            int reste = ligne.getQuantite() - ligne.getQuantiteRecue();
            if (reste > 0) {
                mvtStkService.enregistrerMouvement(
                        ligne.getArticle(), TypeMouvement.ENTREE, reste, null, commande.getCode()
                );
            }
            ligne.setQuantiteRecue(ligne.getQuantite());
        }

        commande.setStatut(StatutCommandeFournisseur.RECUE);
        return toResponseDTO(commandeFournisseurRepository.save(commande));
    }

    /**
     * EN_ATTENTE / RECUE_PARTIELLEMENT -> RECUE_PARTIELLEMENT (§3.5).
     * Reçoit uniquement les quantités transmises : chaque ligne absente de la
     * requête, ou avec quantiteRecue = 0, ne génère aucun mouvement. Les
     * lignes déjà satisfaites ne peuvent pas être "re-reçues". Si toutes les
     * lignes deviennent satisfaites, la commande passe automatiquement à RECUE
     * (équivalent fonctionnel d'une réception complète).
     */
    @Transactional
    public CommandeFournisseurResponseDTO receptionnerPartiellement(Long id, ReceptionPartielleDTO dto) {
        CommandeFournisseur commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeFournisseur.EN_ATTENTE
                && commande.getStatut() != StatutCommandeFournisseur.RECUE_PARTIELLEMENT) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_ATTENTE ou RECUE_PARTIELLEMENT accepte une réception partielle "
                            + "(statut actuel : " + commande.getStatut() + ")"
            );
        }

        // Index des quantités demandées par ligne, pour rejeter les doublons.
        Map<Long, Integer> quantitesDemandees = new HashMap<>();
        for (ReceptionPartielleDTO.LigneRecueDTO ligneDto : dto.lignes()) {
            if (quantitesDemandees.putIfAbsent(ligneDto.ligneId(), ligneDto.quantiteRecue()) != null) {
                throw new IllegalArgumentException("La ligne " + ligneDto.ligneId() + " est présente deux fois dans la requête");
            }
        }

        for (LigneCommandeFournisseur ligne : commande.getLignes()) {
            Integer demandee = quantitesDemandees.get(ligne.getId());
            if (demandee == null || demandee == 0) {
                continue; // ligne non reçue dans cette réception partielle
            }
            int reste = ligne.getQuantite() - ligne.getQuantiteRecue();
            if (demandee > reste) {
                throw new IllegalArgumentException(
                        "Quantité reçue invalide pour l'article " + ligne.getArticle().getDesignation()
                                + " : " + demandee + " demandé(s), " + reste + " restant(s) à réceptionner"
                );
            }
            mvtStkService.enregistrerMouvement(
                    ligne.getArticle(), TypeMouvement.ENTREE, demandee, null, commande.getCode()
            );
            ligne.setQuantiteRecue(ligne.getQuantiteRecue() + demandee);
        }

        boolean toutRecu = commande.getLignes().stream()
                .allMatch(l -> l.getQuantiteRecue() >= l.getQuantite());
        if (toutRecu) {
            commande.setStatut(StatutCommandeFournisseur.RECUE);
        } else {
            commande.setStatut(StatutCommandeFournisseur.RECUE_PARTIELLEMENT);
        }
        return toResponseDTO(commandeFournisseurRepository.save(commande));
    }

    /**
     * EN_ATTENTE -> ANNULEE uniquement. Une commande RECUE_PARTIELLEMENT a
     * déjà généré des entrées de stock : l'annuler nécessiterait des
     * mouvements inverses, hors périmètre pour l'instant.
     */
    public CommandeFournisseurResponseDTO annuler(Long id) {
        CommandeFournisseur commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeFournisseur.EN_ATTENTE) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_ATTENTE peut être annulée (statut actuel : " + commande.getStatut() + ")"
            );
        }
        commande.setStatut(StatutCommandeFournisseur.ANNULEE);
        return toResponseDTO(commandeFournisseurRepository.save(commande));
    }

    // --- Helpers privés ---

    private List<LigneCommandeFournisseur> construireLignes(List<LigneCommandeFournisseurRequestDTO> lignesDto, CommandeFournisseur commande) {
        List<LigneCommandeFournisseur> lignes = new ArrayList<>();
        for (LigneCommandeFournisseurRequestDTO ligneDto : lignesDto) {
            Article article = getArticleOrThrow(ligneDto.articleId());
            LigneCommandeFournisseur ligne = new LigneCommandeFournisseur();
            ligne.setArticle(article);
            ligne.setQuantite(ligneDto.quantite());
            ligne.setPrixUnitaire(article.getPrixUnitaireHt());
            ligne.setCommandeFournisseur(commande);
            lignes.add(ligne);
        }
        return lignes;
    }

    private Fournisseur getFournisseurOrThrow(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = fournisseur.getEntreprise() != null
                && fournisseur.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Fournisseur introuvable avec id=" + id);
        }
        return fournisseur;
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

    private CommandeFournisseur getCommandeOrThrow(Long id) {
        CommandeFournisseur commande = commandeFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = commande.getEntreprise() != null
                && commande.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Commande fournisseur introuvable avec id=" + id);
        }
        return commande;
    }

    private CommandeFournisseurResponseDTO toResponseDTO(CommandeFournisseur commande) {
        List<LigneCommandeFournisseurResponseDTO> lignes = commande.getLignes().stream()
                .map(l -> new LigneCommandeFournisseurResponseDTO(
                        l.getId(),
                        l.getArticle().getId(),
                        l.getArticle().getDesignation(),
                        l.getQuantite(),
                        l.getQuantiteRecue(),
                        l.getPrixUnitaire(),
                        l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite()))
                ))
                .toList();

        BigDecimal total = lignes.stream()
                .map(LigneCommandeFournisseurResponseDTO::sousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CommandeFournisseurResponseDTO(
                commande.getId(),
                commande.getCode(),
                commande.getDateCommande(),
                commande.getStatut(),
                commande.getFournisseur().getId(),
                commande.getFournisseur().getNom(),
                lignes,
                total
        );
    }
}
