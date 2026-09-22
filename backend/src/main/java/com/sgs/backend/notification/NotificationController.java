package com.sgs.backend.notification;

import com.sgs.backend.notification.dto.NotificationDTO;
import com.sgs.backend.stock.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le cahier des charges (§3.8) ne prévoit qu'un seul déclencheur de
 * notification "in-app" : le stock qui atteint ou passe sous son seuil
 * (RG-09). Pas d'entité Notification ni d'état "lu/non lu" -- l'alerte est
 * calculée à la volée depuis StockService à chaque appel, ce qui évite
 * un flux d'écriture supplémentaire (et le risque de désynchronisation
 * avec le stock réel) pour un besoin qui reste, pour l'instant, uniquement
 * un signal visuel côté frontend.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Alertes in-app (actuellement : stock sous le seuil minimum)")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "Notifications actives")
    public ResponseEntity<List<NotificationDTO>> findAll() {
        List<NotificationDTO> notifications = stockService.alertes().stream()
                .map(a -> new NotificationDTO(
                        "STOCK_BAS",
                        "Stock bas pour '" + a.designation() + "' (" + a.stockActuel() + " / seuil " + a.seuilMin() + ")",
                        a.articleId(),
                        a.designation()
                ))
                .toList();
        return ResponseEntity.ok(notifications);
    }
}
