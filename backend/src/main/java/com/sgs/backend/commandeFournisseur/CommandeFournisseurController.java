package com.sgs.backend.commandeFournisseur;

import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurResponseDTO;
import com.sgs.backend.commandeFournisseur.dto.ReceptionPartielleDTO;
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
@RequestMapping("/api/commandes-fournisseur")
@RequiredArgsConstructor
@Tag(name = "Commandes fournisseur", description = "Commandes d'achat, dont la réception génère une entrée de stock")
@SecurityRequirement(name = "bearerAuth")
public class CommandeFournisseurController {

    private final CommandeFournisseurService commandeFournisseurService;

    @GetMapping
    @Operation(summary = "Lister les commandes fournisseur")
    public ResponseEntity<List<CommandeFournisseurResponseDTO>> findAll() {
        return ResponseEntity.ok(commandeFournisseurService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande fournisseur")
    public ResponseEntity<CommandeFournisseurResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeFournisseurService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une commande fournisseur", description = "Commande + lignes en une seule requête, statut initial EN_ATTENTE.")
    public ResponseEntity<CommandeFournisseurResponseDTO> create(@Valid @RequestBody CommandeFournisseurRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeFournisseurService.create(dto));
    }

    @PutMapping("/{id}/receptionner")
    @Operation(summary = "Réceptionner la commande", description = "Génère une entrée de stock par ligne. Refusé si déjà reçue.")
    public ResponseEntity<CommandeFournisseurResponseDTO> receptionner(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeFournisseurService.receptionner(id));
    }

    @PutMapping("/{id}/receptionner-partiel")
    @Operation(summary = "Réceptionner partiellement la commande",
            description = "Enregistre les quantités effectivement reçues, ligne par ligne (§3.5). "
                    + "Les lignes absentes de la requête ne reçoivent rien ; on ne peut pas recevoir plus que "
                    + "le solde restant. Passe automatiquement à RECUE quand toutes les lignes sont satisfaites.")
    public ResponseEntity<CommandeFournisseurResponseDTO> receptionnerPartiellement(
            @Parameter(description = "ID de la commande") @PathVariable Long id,
            @Valid @RequestBody ReceptionPartielleDTO dto
    ) {
        return ResponseEntity.ok(commandeFournisseurService.receptionnerPartiellement(id, dto));
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler la commande", description = "Possible uniquement tant que la commande est EN_ATTENTE (pas RECUE_PARTIELLEMENT : le stock a déjà bougé).")
    public ResponseEntity<CommandeFournisseurResponseDTO> annuler(
            @Parameter(description = "ID de la commande") @PathVariable Long id
    ) {
        return ResponseEntity.ok(commandeFournisseurService.annuler(id));
    }
}
