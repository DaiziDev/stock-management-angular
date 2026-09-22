package com.sgs.backend.vente;

import com.sgs.backend.vente.dto.VenteRequestDTO;
import com.sgs.backend.vente.dto.VenteResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Point de vente : décrémente le stock immédiatement à la création")
@SecurityRequirement(name = "bearerAuth")
public class VenteController {

    private final VenteService venteService;

    @GetMapping
    @Operation(summary = "Lister les ventes")
    public ResponseEntity<List<VenteResponseDTO>> findAll() {
        return ResponseEntity.ok(venteService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une vente")
    public ResponseEntity<VenteResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(venteService.findById(id));
    }

    @PostMapping
    @Operation(
            summary = "Encaisser une vente",
            description = "Vente + lignes en une seule requête. Décrémente le stock immédiatement. " +
                    "Refusée en bloc (409) si le stock est insuffisant pour au moins une ligne."
    )
    public ResponseEntity<VenteResponseDTO> create(@Valid @RequestBody VenteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venteService.create(dto));
    }
}
