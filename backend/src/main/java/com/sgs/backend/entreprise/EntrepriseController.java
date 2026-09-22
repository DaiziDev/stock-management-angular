package com.sgs.backend.entreprise;

import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurService;
import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import com.sgs.backend.entreprise.dto.EntrepriseUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des entreprises clientes — réservée au SUPER_ADMIN (plateforme).
 *
 * C'est la porte d'entrée du multi-tenant : c'est l'opérateur de la
 * plateforme qui onboard une entreprise cliente (puis lui crée son premier
 * ADMIN via POST /api/plateforme/entreprises/{id}/admins). Un tenant ne
 * liste ni ne crée d'entreprises : sinon un simple VENDEUR connecté pourrait
 * créer "Telcar Ltd" et s'y rattacher -- contournement trivial de RG-10.
 * (Même style de vérification manuelle que AuthController/UtilisateurController :
 * pas d'autorisation par annotation dans ce codebase.)
 */
@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "Gestion des entreprises clientes (SUPER_ADMIN uniquement)")
@SecurityRequirement(name = "bearerAuth")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;
    private final UtilisateurService utilisateurService;

    @GetMapping
    @Operation(summary = "Lister toutes les entreprises clientes")
    public ResponseEntity<List<EntrepriseResponseDTO>> findAll(@AuthenticationPrincipal UserDetails currentUser) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(entrepriseService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une entreprise cliente")
    public ResponseEntity<EntrepriseResponseDTO> findById(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(entrepriseService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une entreprise cliente", description = "Réservé au SUPER_ADMIN (plateforme).")
    public ResponseEntity<EntrepriseResponseDTO> create(
            @Parameter(description = "Données de l'entreprise", required = true)
            @Valid @RequestBody EntrepriseRequestDTO dto,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        EntrepriseResponseDTO created = entrepriseService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une entreprise cliente", description = "Coordonnées uniquement : le nom (identifiant de cloisonnement) n'est pas modifiable.")
    public ResponseEntity<EntrepriseResponseDTO> update(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id,
            @Parameter(description = "Nouvelles coordonnées", required = true)
            @Valid @RequestBody EntrepriseUpdateDTO dto,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une entreprise cliente")
    @ApiResponse(responseCode = "204", description = "Entreprise supprimée")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!estSuperAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Un utilisateur d'entreprise (même ADMIN) n'a pas accès à ce module. */
    private boolean estSuperAdmin(UserDetails currentUser) {
        Utilisateur appelant = utilisateurService.findByLogin(currentUser.getUsername());
        return appelant.getRole() == UserRole.SUPER_ADMIN;
    }
}
