package com.sgs.backend.common;

import com.sgs.backend.auth.RefreshTokenInvalidException;
import com.sgs.backend.mvtStk.StockInsuffisantException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

// @RestControllerAdvice intercepte les exceptions levées PAR N'IMPORTE QUEL
// controller de l'application. Sans ça, une exception non gérée renvoie
// une page d'erreur Spring par défaut (ou une stacktrace) au lieu d'un JSON propre.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ressource introuvable (ex: GET /api/categories/999 qui n'existe pas) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Identifiants invalides sur POST /api/auth/login (authenticationManager.authenticate()
    // lève BadCredentialsException, une sous-classe d'AuthenticationException) -> 401.
    // Sans ce handler, Spring MVC ne sait pas convertir cette exception en réponse
    // HTTP propre et renvoie un 500 générique -- trompeur pour un simple mauvais mot de passe.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Identifiant ou mot de passe incorrect",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Refresh token inconnu, expiré ou rejoué -> 401 : le frontend doit
    // rediriger vers /login (pas retenter). 401 et non 403 : l'identité
    // n'a pas pu être rétablie, c'est le même sens qu'un access token expiré.
    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<ApiError> handleRefreshTokenInvalid(RefreshTokenInvalidException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Refus métier attendu : code déjà utilisé, statut de commande incompatible
    // avec l'action demandée ("déjà validée", "déjà reçue"...) -> 400.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Stock insuffisant pour une SORTIE (vente, validation de commande client) -> 409.
    // 409 (Conflict) plutôt que 400 : la requête est valide, c'est l'état actuel
    // du stock qui empêche de l'honorer -- RG-05 du cahier des charges.
    @ExceptionHandler(StockInsuffisantException.class)
    public ResponseEntity<ApiError> handleStockInsuffisant(StockInsuffisantException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Violation de contrainte en base (nom d'entreprise unique, login unique,
    // FK...) -> 409. Sans ce handler, Spring renvoyait un 500 au corps VIDE :
    // le frontend ne pouvait pas afficher pourquoi l'onboarding échouait.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String message = ex.getMostSpecificCause().getMessage() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Conflit de données : cette ressource existe déjà ou viole une contrainte.";
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                message,
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Erreurs de validation (@NotNull, @NotBlank... sur un DTO annoté @Valid) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                details,
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
