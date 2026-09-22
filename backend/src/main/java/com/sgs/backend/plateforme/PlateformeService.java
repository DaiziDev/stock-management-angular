package com.sgs.backend.plateforme;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.client.ClientRepository;
import com.sgs.backend.commande.StatutCommandeClient;
import com.sgs.backend.commandeClient.CommandeClientRepository;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import com.sgs.backend.plateforme.dto.PlateformeStatsDTO;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import com.sgs.backend.utilisateur.UtilisateurService;
import com.sgs.backend.vente.Vente;
import com.sgs.backend.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * Logique métier de la PLATEFORME (vue SUPER_ADMIN) : indicateurs globaux
 * et onboarding des entreprises clientes.
 *
 * L'onboarding en une transaction résout la séquence la plus utile de la
 * porte d'entrée (cf. flux logique fonctionnel, flux 3+4) : créer
 * l'entreprise PUIS son premier ADMIN, soit tout est enregistré soit rien --
 * impossible de finir avec une entreprise sans compte (inutilisable) ou un
 * compte orphelin (violation RG-01). Le mot de passe de l'admin est hashé
 * par UtilisateurService.create.
 */
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;
    private final ArticleRepository articleRepository;
    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;
    private final CommandeClientRepository commandeClientRepository;

    /**
     * readOnly = true : les collections lazy (Vente.lignes) sont parcourues
     * dans la même transaction Hibernate -- sans ça, LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public PlateformeStatsDTO stats() {
        // Le calcul du CA du mois n'a de sens que sur les ventes de la
        // période : une requête filtrée plutôt que de charger l'historique.
        Instant debutDuMois = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Vente> ventesDuMois = venteRepository.findByEntrepriseIsNotNullAndDateVenteGreaterThanEqual(debutDuMois);

        // CA TTC du mois : la ligne de vente copie le prix au moment de la
        // vente, on somme donc prix x quantité (chargement paresseux des
        // lignes, volumétrie modérée : uniquement les ventes de la période).
        BigDecimal chiffreAffaires = ventesDuMois.stream()
                .flatMap(v -> v.getLignes().stream())
                .map(l -> l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Entreprise> entreprises = entrepriseRepository.findAll();
        List<PlateformeStatsDTO.EntrepriseRecenteDTO> dernieres = entreprises.stream()
                .sorted(Comparator.comparing(Entreprise::getCreatedAt).reversed())
                .limit(5)
                .map(e -> new PlateformeStatsDTO.EntrepriseRecenteDTO(
                        e.getId(),
                        e.getNom(),
                        e.getMail(),
                        e.getAdresse() != null ? e.getAdresse().getVille() : null,
                        utilisateurRepository.countByEntrepriseId(e.getId()),
                        e.getCreatedAt()
                ))
                .toList();

        return new PlateformeStatsDTO(
                entreprises.size(),
                utilisateurRepository.count(),
                utilisateurRepository.countByRole(UserRole.ADMIN),
                clientRepository.countByEntrepriseIsNotNull(),
                articleRepository.countByEntrepriseIsNotNull(),
                venteRepository.countByEntrepriseIsNotNull(),
                ventesDuMois.size(),
                chiffreAffaires,
                articleRepository.countAlertes(),
                commandeClientRepository.countByEntrepriseIsNotNullAndStatut(StatutCommandeClient.EN_COURS),
                entreprises.stream().filter(e -> e.getCreatedAt().isAfter(
                        LocalDate.now().withDayOfMonth(1).atStartOfDay())).count(),
                dernieres
        );
    }

    @Transactional
    public EntrepriseResponseDTO onboarderEntreprise(EntrepriseRequestDTO entrepriseDto, Utilisateur adminACreer) {
        // 1. L'entreprise, avec TOUTES ses coordonnées (le nom est déjà validé
        //    unique par la contrainte en base : une entreprise homonyme lèvera
        //    une erreur d'intégrité -> rollback complet de la transaction).
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(entrepriseDto.nom());
        entreprise.setMail(entrepriseDto.mail());
        entreprise.setNumTel(entrepriseDto.numTel());
        entreprise.setAdresse(new Adresse(
                entrepriseDto.adresse1(), entrepriseDto.adresse2(), entrepriseDto.ville(),
                entrepriseDto.codePostal(), entrepriseDto.pays()
        ));
        Entreprise saved = entrepriseRepository.save(entreprise);

        // 2. Son premier compte ADMIN, rattaché à cette entreprise
        adminACreer.setEntreprise(saved);
        utilisateurService.create(adminACreer);

        // 3. Réponse enrichie du compteur (l'admin vient d'être créé : 1)
        Adresse adresse = saved.getAdresse();
        return new EntrepriseResponseDTO(
                saved.getId(),
                saved.getNom(),
                adresse != null ? adresse.getAdresse1() : null,
                adresse != null ? adresse.getAdresse2() : null,
                adresse != null ? adresse.getVille() : null,
                adresse != null ? adresse.getCodePostal() : null,
                adresse != null ? adresse.getPays() : null,
                saved.getMail(),
                saved.getNumTel(),
                1L
        );
    }
}
