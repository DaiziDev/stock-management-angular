package com.sgs.backend.commandeClient;

import com.sgs.backend.commandeClient.dto.CommandeClientRequestDTO;
import com.sgs.backend.commandeClient.dto.CommandeClientResponseDTO;
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
@RequestMapping("/api/commandes-client")
@RequiredArgsConstructor
@Tag(name = "Commandes client", description = "Commandes passées par un client, avant transformation en sortie de stock")
@SecurityRequirement(name = "bearerAuth")
public class CommandeClientController {

    private final CommandeClientService commandeClientService;

    @GetMapping
    @Operation(summary = "Lister les commandes client")
    public ResponseEntity<List<CommandeClientResponseDTO>> findAll() {
        return ResponseEntity.ok(commandeClientService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande client")
    public ResponseEntity<CommandeClientResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeClientService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une commande client", description = "Commande + lignes en une seule requête, statut initial EN_COURS.")
    public ResponseEntity<CommandeClientResponseDTO> create(@Valid @RequestBody CommandeClientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeClientService.create(dto));
    }

    @PutMapping("/{id}/valider")
    @Operation(summary = "Valider la commande", description = "Génère une sortie de stock par ligne. Refusé si le stock est insuffisant (409).")
    public ResponseEntity<CommandeClientResponseDTO> valider(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeClientService.valider(id));
    }

    @PutMapping("/{id}/expedier")
    @Operation(summary = "Marquer la commande comme expédiée", description = "VALIDEE -> EXPEDIEE. Aucun impact stock : les sorties ont été générées à la validation.")
    public ResponseEntity<CommandeClientResponseDTO> expedier(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeClientService.expedier(id));
    }

    @PutMapping("/{id}/livrer")
    @Operation(summary = "Marquer la commande comme livrée", description = "EXPEDIEE -> LIVREE (raccourci toléré depuis VALIDEE). Aucun impact stock.")
    public ResponseEntity<CommandeClientResponseDTO> livrer(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeClientService.livrer(id));
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler la commande", description = "Possible uniquement tant que la commande est EN_COURS.")
    public ResponseEntity<CommandeClientResponseDTO> annuler(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeClientService.annuler(id));
    }
}
