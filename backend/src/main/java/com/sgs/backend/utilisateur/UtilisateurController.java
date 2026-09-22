package com.sgs.backend.utilisateur;

import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.dto.UtilisateurResponseDTO;
import com.sgs.backend.utilisateur.dto.UtilisateurUpdateDTO;
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

import java.util.List;

/**
 * Réservé aux ADMIN D'ENTREPRISE -- la création reste sur POST /api/auth/register
 * (qui applique déjà cette même vérification), ce controller ne couvre que
 * lister/consulter/modifier/supprimer un compte de la même entreprise.
 * Le SUPER_ADMIN n'est PAS concerné : il n'appartient à aucune entreprise,
 * il gère les comptes via le module plateforme (sinon entreprise.getId()
 * serait null ici et ferait planter la requête).
 * Pas de @PreAuthorize ici : ce codebase n'utilise pas encore
 * l'autorisation par annotation, on reste sur le même style de vérification
 * manuelle que le reste du code (AuthController.register).
 */
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des comptes (ADMIN uniquement)")
@SecurityRequirement(name = "bearerAuth")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @Operation(summary = "Lister les utilisateurs de mon entreprise")
    public ResponseEntity<List<UtilisateurResponseDTO>> findAll(@AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        if (moi.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<UtilisateurResponseDTO> utilisateurs = utilisateurService.findAllByEntreprise(moi.getEntreprise().getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public ResponseEntity<UtilisateurResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        if (moi.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toResponseDTO(utilisateurService.findByIdAndEntreprise(id, moi.getEntreprise().getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un utilisateur", description = "Nom, prénom, contact et rôle. Ni login ni mot de passe (endpoints séparés).")
    public ResponseEntity<UtilisateurResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdateDTO dto,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        if (moi.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Utilisateur updated = utilisateurService.update(id, moi.getEntreprise().getId(), dto);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        if (moi.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        utilisateurService.delete(id, moi.getEntreprise().getId());
        return ResponseEntity.noContent().build();
    }

    private UtilisateurResponseDTO toResponseDTO(Utilisateur utilisateur) {
        return new UtilisateurResponseDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getMail(),
                utilisateur.getNumTel(),
                utilisateur.getRole()
        );
    }
}
