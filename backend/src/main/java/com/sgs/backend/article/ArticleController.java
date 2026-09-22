package com.sgs.backend.article;

import com.sgs.backend.article.dto.ArticleRequestDTO;
import com.sgs.backend.article.dto.ArticleResponseDTO;
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
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "Gestion du catalogue d'articles")
@SecurityRequirement(name = "bearerAuth")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "Lister tous les articles", description = "Retourne la liste complète des articles.")
    public ResponseEntity<List<ArticleResponseDTO>> findAll() {
        return ResponseEntity.ok(articleService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un article", description = "Retourne un article par son ID.")
    public ResponseEntity<ArticleResponseDTO> findById(
            @Parameter(description = "ID de l'article") @PathVariable Long id
    ) {
        return ResponseEntity.ok(articleService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un article", description = "Ajoute un nouvel article au catalogue.")
    public ResponseEntity<ArticleResponseDTO> create(
            @Parameter(description = "Données de l'article", required = true)
            @Valid @RequestBody ArticleRequestDTO dto
    ) {
        ArticleResponseDTO created = articleService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un article", description = "Met à jour les informations d'un article existant.")
    public ResponseEntity<ArticleResponseDTO> update(
            @Parameter(description = "ID de l'article") @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @Valid @RequestBody ArticleRequestDTO dto
    ) {
        return ResponseEntity.ok(articleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article", description = "Supprime un article du catalogue.")
    @ApiResponse(responseCode = "204", description = "Article supprimé")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de l'article") @PathVariable Long id
    ) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
