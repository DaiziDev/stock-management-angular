package com.sgs.backend.client;

import com.sgs.backend.client.dto.ClientRequestDTO;
import com.sgs.backend.client.dto.ClientResponseDTO;
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
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestion des fiches clients")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @Operation(summary = "Lister tous les clients")
    public ResponseEntity<List<ClientResponseDTO>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un client")
    public ResponseEntity<ClientResponseDTO> findById(
            @Parameter(description = "ID du client") @PathVariable Long id
    ) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un client")
    public ResponseEntity<ClientResponseDTO> create(
            @Parameter(description = "Données du client", required = true)
            @Valid @RequestBody ClientRequestDTO dto
    ) {
        ClientResponseDTO created = clientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un client")
    public ResponseEntity<ClientResponseDTO> update(
            @Parameter(description = "ID du client") @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @Valid @RequestBody ClientRequestDTO dto
    ) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un client")
    @ApiResponse(responseCode = "204", description = "Client supprimé")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du client") @PathVariable Long id
    ) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
