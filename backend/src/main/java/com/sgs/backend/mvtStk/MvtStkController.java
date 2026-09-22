package com.sgs.backend.mvtStk;

import com.sgs.backend.mvtStk.dto.MvtStkRequestDTO;
import com.sgs.backend.mvtStk.dto.MvtStkResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mouvements-stock")
@RequiredArgsConstructor
@Tag(name = "Mouvements de stock", description = "Historique des entrées/sorties et ajustements manuels")
@SecurityRequirement(name = "bearerAuth")
public class MvtStkController {

    private final MvtStkService mvtStkService;

    @GetMapping
    @Operation(summary = "Historique des mouvements", description = "Filtrable par article et/ou par type.")
    public ResponseEntity<List<MvtStkResponseDTO>> findAll(
            @Parameter(description = "Filtrer par article") @RequestParam(required = false) Long articleId,
            @Parameter(description = "Filtrer par type (ENTREE/SORTIE/AJUSTEMENT)") @RequestParam(required = false) TypeMouvement type
    ) {
        return ResponseEntity.ok(mvtStkService.findAll(articleId, type));
    }

    @PostMapping
    @Operation(
            summary = "Ajustement manuel de stock",
            description = "Correction d'inventaire avec motif obligatoire. Quantité signée (+/-)."
    )
    public ResponseEntity<MvtStkResponseDTO> creerAjustement(
            @Valid @RequestBody MvtStkRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mvtStkService.creerAjustement(dto));
    }
}
