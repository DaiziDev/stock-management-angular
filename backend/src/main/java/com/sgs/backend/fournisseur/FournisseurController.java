package com.sgs.backend.fournisseur;

import com.sgs.backend.fournisseur.dto.FournisseurRequestDTO;
import com.sgs.backend.fournisseur.dto.FournisseurResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Gestion des fiches fournisseurs")
@SecurityRequirement(name = "bearerAuth")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    @GetMapping
    @Operation(summary = "Lister tous les fournisseurs")
    public ResponseEntity<List<FournisseurResponseDTO>> findAll() {
        return ResponseEntity.ok(fournisseurService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un fournisseur")
    public ResponseEntity<FournisseurResponseDTO> findById(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id
    ) {
        return ResponseEntity.ok(fournisseurService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un fournisseur")
    public ResponseEntity<FournisseurResponseDTO> create(
            @Parameter(description = "Données du fournisseur", required = true)
            @Valid @RequestBody FournisseurRequestDTO dto
    ) {
        FournisseurResponseDTO created = fournisseurService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un fournisseur")
    public ResponseEntity<FournisseurResponseDTO> update(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @Valid @RequestBody FournisseurRequestDTO dto
    ) {
        return ResponseEntity.ok(fournisseurService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un fournisseur")
    @ApiResponse(responseCode = "204", description = "Fournisseur supprimé")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id
    ) {
        fournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
