package com.sgs.backend.plateforme;

import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import com.sgs.backend.plateforme.dto.AdminEntrepriseRequestDTO;
import com.sgs.backend.plateforme.dto.PlateformeStatsDTO;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de la PLATEFORME -- exclusivement réservés au SUPER_ADMIN.
 *
 * C'est le backoffice de l'opérateur (nous) : vue d'ensemble des entreprises
 * clientes et onboarding ("créer la boîte + son patron" en une transaction).
 * Un utilisateur d'entreprise (même ADMIN) reçoit 403 sur tout ce module :
 * la vue d'ensemble multi-entreprises n'a de sens que pour la plateforme.
 */
@RestController
@RequestMapping("/api/plateforme")
@RequiredArgsConstructor
@Tag(name = "Plateforme", description = "Backoffice de l'opérateur : stats globales et onboarding des entreprises clientes (SUPER_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class PlateformeController {

    private final PlateformeService plateformeService;
    private final UtilisateurService utilisateurService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales de la plateforme")
    public ResponseEntity<PlateformeStatsDTO> stats(@AuthenticationPrincipal UserDetails currentUser) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(plateformeService.stats());
    }

    /**
     * POST /api/plateforme/entreprises
     * Crée l'entreprise cliente ET son premier ADMIN en une transaction.
     * (Raccourci de POST /api/entreprises + POST /api/auth/register, pour le
     * formulaire d'onboarding de la console plateforme.)
     */
    @PostMapping("/entreprises")
    @Operation(summary = "Onboarder une entreprise cliente", description = "Crée l'entreprise et son premier compte ADMIN en une seule transaction.")
    public ResponseEntity<EntrepriseResponseDTO> onboarder(
            @Valid @RequestBody AdminEntrepriseRequestDTO dto,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Utilisateur admin = new Utilisateur();
        admin.setNom(dto.adminNom());
        admin.setPrenom(dto.adminPrenom());
        admin.setLogin(dto.adminLogin());
        admin.setMotDePasse(dto.adminMotDePasse());
        admin.setMail(dto.adminMail());
        admin.setNumTel(dto.adminNumTel());
        admin.setRole(AdminEntrepriseRequestDTO.ROLE_IMPOSE);

        EntrepriseResponseDTO created = plateformeService.onboarderEntreprise(
                new com.sgs.backend.entreprise.dto.EntrepriseRequestDTO(
                        dto.nomEntreprise(),
                        dto.adresse1(),
                        dto.adresse2(),
                        dto.ville(),
                        dto.codePostal(),
                        dto.pays(),
                        dto.mailEntreprise(),
                        dto.numTelEntreprise()
                ),
                admin
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Un utilisateur d'entreprise (même ADMIN) n'a pas accès à ce module. */
    private boolean estSuperAdmin(UserDetails currentUser) {
        Utilisateur appelant = utilisateurService.findByLogin(currentUser.getUsername());
        return appelant.getRole() == UserRole.SUPER_ADMIN;
    }
}
