package com.sgs.backend.categorie;

import com.sgs.backend.categorie.dto.CategorieRequestDTO;
import com.sgs.backend.categorie.dto.CategorieResponseDTO;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Gestion des catégories de produits")
@SecurityRequirement(name = "bearerAuth")
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    @Operation(summary = "Lister toutes les catégories")
    public ResponseEntity<List<CategorieResponseDTO>> findAll() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une catégorie")
    public ResponseEntity<CategorieResponseDTO> findById(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id
    ) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une catégorie")
    public ResponseEntity<CategorieResponseDTO> create(
            @Parameter(description = "Données de la catégorie", required = true)
            @Valid @RequestBody CategorieRequestDTO dto
    ) {
        CategorieResponseDTO created = categorieService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une catégorie")
    public ResponseEntity<CategorieResponseDTO> update(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @Valid @RequestBody CategorieRequestDTO dto
    ) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie")
    @ApiResponse(responseCode = "204", description = "Catégorie supprimée")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la catégorie") @PathVariable Long id
    ) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
