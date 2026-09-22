package com.sgs.backend.dashboard;

import com.sgs.backend.dashboard.dto.DashboardKpisDTO;
import com.sgs.backend.dashboard.dto.GraphiquesResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Indicateurs clés agrégés (stock, commandes, ventes)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    @Operation(summary = "KPIs du tableau de bord", description = "Valeur du stock, alertes, commandes en cours, chiffre d'affaires du mois.")
    public ResponseEntity<DashboardKpisDTO> kpis() {
        return ResponseEntity.ok(dashboardService.kpis());
    }

    @GetMapping("/graphiques")
    @Operation(summary = "Données des graphiques", description = "Évolution des entrées/sorties de stock sur 30 jours et top 5 des articles les plus vendus.")
    public ResponseEntity<GraphiquesResponseDTO> graphiques() {
        return ResponseEntity.ok(dashboardService.graphiques());
    }
}
