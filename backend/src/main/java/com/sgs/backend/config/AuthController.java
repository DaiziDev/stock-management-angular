package com.sgs.backend.config;

import com.sgs.backend.auth.AuthTokensDTO;
import com.sgs.backend.auth.AuthTokensRequest;
import com.sgs.backend.auth.RefreshTokenService;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.dto.CurrentUserResponse;
import com.sgs.backend.config.dto.LoginRequest;
import com.sgs.backend.config.dto.LoginResponse;
import com.sgs.backend.config.dto.RegisterRequest;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — endpoints d'authentification.
 *
 * Ce controller est le SEUL endpoint public (hors Swagger).
 * Tous les autres endpoints de l'API nécessitent un JWT valide.
 *
 * Flow de connexion (§6.3 du cahier des charges : couple access + refresh) :
 *   1. Frontend envoie POST /api/auth/login { login, motDePasse }
 *   2. Spring Security vérifie le mot de passe via AuthenticationManager
 *   3. Si OK → on génère un JWT (24h) + un refresh token opaque (7 jours)
 *   4. On retourne le couple de tokens + les infos utilisateur
 *   5. Frontend stocke les deux tokens dans localStorage
 *   6. À chaque requête future → header Authorization: Bearer <access>
 *   7. JWT expiré → POST /api/auth/refresh { refreshToken } renvoie un
 *      NOUVEAU couple (rotation) sans redemander les identifiants
 *   8. Déconnexion → POST /api/auth/logout révoque le refresh token
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion, inscription et profil utilisateur")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;
    private final UserDetailsService userDetailsService;
    private final EntrepriseRepository entrepriseRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * POST /api/auth/login
     *
     * Authentifie un utilisateur et retourne un JWT.
     *
     * Le flow interne :
     * 1. AuthenticationManager vérifie login + mot de passe via UserDetailsService
     * 2. Si les identifiants sont corrects → on obtient un objet Authentication
     * 3. On génère un JWT contenant l'email, le rôle et l'entrepriseId
     * 4. On retourne le token + les infos utilisateur (sans le mot de passe)
     */
    @PostMapping("/login")
    @Operation(
            summary = "Connexion",
            description = "Authentifie un utilisateur avec son login et mot de passe. " +
                    "Retourne un token JWT à utiliser dans le header Authorization pour les requêtes suivantes.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Connexion réussie — token JWT retourné"),
                    @ApiResponse(responseCode = "403", description = "Identifiants incorrects")
            }
    )
    public ResponseEntity<AuthTokensDTO> login(
            @Parameter(description = "Identifiants de connexion", required = true)
            @Valid @RequestBody LoginRequest request
    ) {
        // Spring Security vérifie le login + mot de passe
        // Si échoue → lance BadCredentialsException (403 automatique)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.motDePasse()
                )
        );

        // Charger les détails complets de l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Utilisateur utilisateur = utilisateurService.findByLogin(userDetails.getUsername());

        // Générer le JWT avec les infos nécessaires
        String token = jwtUtil.generateToken(
                utilisateur.getLogin(),
                utilisateur.getRole().name(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );

        // Construire la réponse (token + infos utilisateur)
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getRole(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null,
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getNom() : null
        );

        String refreshToken = refreshTokenService.emettrePour(utilisateur);
        return ResponseEntity.ok(new AuthTokensDTO(token, refreshToken, userInfo));
    }

    /**
     * POST /api/auth/refresh
     *
     * Échange un refresh token valide contre un NOUVEAU couple
     * access/refresh (rotation : l'ancien refresh token est consommé).
     * C'est ce qui permet de prolonger la session sans redemander les
     * identifiants quand le JWT de 24h expire.
     *
     * Public (pas de JWT, forcément : il est expiré quand on appelle).
     * Refresh token inconnu/expiré/rejoué -> 401 (RefreshTokenInvalidException).
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Renouveler la session",
            description = "Échange un refresh token valide contre un nouveau couple JWT + refresh token " +
                    "(rotation : l'ancien refresh token devient inutilisable).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Nouveau couple de tokens retourné"),
                    @ApiResponse(responseCode = "401", description = "Refresh token inconnu, expiré ou déjà utilisé")
            }
    )
    public ResponseEntity<AuthTokensDTO> refresh(
            @Parameter(description = "Refresh token reçu au login", required = true)
            @Valid @RequestBody AuthTokensRequest request
    ) {
        RefreshTokenService.AuthTokens renouvellement = refreshTokenService.renouveler(request.refreshToken());
        Utilisateur utilisateur = renouvellement.utilisateur();

        String nouveauJwt = jwtUtil.generateToken(
                utilisateur.getLogin(),
                utilisateur.getRole().name(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );

        return ResponseEntity.ok(new AuthTokensDTO(
                nouveauJwt,
                renouvellement.refreshClair(),
                new LoginResponse.UserInfo(
                        utilisateur.getId(),
                        utilisateur.getNom(),
                        utilisateur.getPrenom(),
                        utilisateur.getLogin(),
                        utilisateur.getRole(),
                        utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null,
                        utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getNom() : null
                )
        ));
    }

    /**
     * POST /api/auth/logout
     *
     * Révoque le refresh token reçu : la session ne peut plus être
     * prolongée, l'utilisateur sera reconnecté au plus tard à
     * l'expiration du JWT courant (24h max, en pratique sa prochaine
     * requête après fermeture n'a plus de sens métier de toute façon).
     *
     * Idempotent : un token déjà révoqué/inconnu répond 204 quand même
     * (le but est que la session meure, elle est déjà morte).
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion",
            description = "Révoque le refresh token fourni. Idempotent.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Refresh token révoqué (ou déjà inconnu)")
            }
    )
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh token à révoquer", required = true)
            @Valid @RequestBody AuthTokensRequest request
    ) {
        refreshTokenService.revoquer(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/register
     *
     * Crée un nouvel utilisateur. Réservé aux ADMIN uniquement.
     *
     * Le mot de passe est hashé côté backend (jamais en clair en base).
     */
    @PostMapping("/register")
    @Operation(
            summary = "Inscription (Admin uniquement)",
            description = "Crée un nouvel utilisateur. Réservé aux administrateurs.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Utilisateur créé"),
                    @ApiResponse(responseCode = "403", description = "Accès refusé (non admin)")
            }
    )
    public ResponseEntity<AuthTokensDTO> register(
            @Parameter(description = "Données du nouvel utilisateur", required = true)
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        // Seul un ADMIN (d'entreprise) ou le SUPER_ADMIN (plateforme) peut
        // créer des comptes. GESTIONNAIRE/VENDEUR sont refusés.
        Utilisateur appelant = utilisateurService.findByLogin(currentUser.getUsername());
        boolean estSuperAdmin = appelant.getRole() == UserRole.SUPER_ADMIN;
        if (!estSuperAdmin && appelant.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Un ADMIN d'entreprise ne peut créer des comptes QUE dans SA propre
        // entreprise : sinon l'admin de "DOVV Essos" pourrait s'auto-créer un
        // compte ADMIN chez "Telcar" -- une fuite de l'isolation RG-10. Seul
        // le SUPER_ADMIN (plateforme) peut onboarder un compte dans n'importe
        // quelle entreprise : c'est son rôle (créer le premier ADMIN d'une
        // entreprise cliente).
        if (!estSuperAdmin) {
            Long entrepriseAppelantId = appelant.getEntreprise() != null ? appelant.getEntreprise().getId() : null;
            if (!request.entrepriseId().equals(entrepriseAppelantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        // Vérifier que l'entreprise existe
        Entreprise entreprise = entrepriseRepository.findById(request.entrepriseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entreprise introuvable avec id=" + request.entrepriseId()
                ));

        // Créer l'utilisateur (le mot de passe sera hashé dans UtilisateurService.create)
        Utilisateur newUser = new Utilisateur();
        newUser.setNom(request.nom());
        newUser.setPrenom(request.prenom());
        newUser.setLogin(request.login());
        newUser.setMotDePasse(request.motDePasse());
        newUser.setMail(request.mail());
        newUser.setNumTel(request.numTel());
        newUser.setRole(request.role());
        newUser.setEntreprise(entreprise);

        Utilisateur saved = utilisateurService.create(newUser);

        // Générer un token pour le nouvel utilisateur
        String token = jwtUtil.generateToken(
                saved.getLogin(),
                saved.getRole().name(),
                saved.getEntreprise() != null ? saved.getEntreprise().getId() : null
        );

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                saved.getId(),
                saved.getNom(),
                saved.getPrenom(),
                saved.getLogin(),
                saved.getRole(),
                saved.getEntreprise() != null ? saved.getEntreprise().getId() : null,
                saved.getEntreprise() != null ? saved.getEntreprise().getNom() : null
        );

        String refreshToken = refreshTokenService.emettrePour(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthTokensDTO(token, refreshToken, userInfo));
    }

    /**
     * GET /api/auth/me
     *
     * Retourne les informations de l'utilisateur connecté.
     * Le token JWT est automatiquement vérifié par JwtAuthFilter.
     *
     * Utilisé par le frontend lors du refresh de la page
     * pour recharger le profil sans re-demander les identifiants.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Mon profil",
            description = "Retourne les informations de l'utilisateur connecté (déduit du token JWT).",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profil retourné"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié")
            }
    )
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Utilisateur utilisateur = utilisateurService.findByLogin(userDetails.getUsername());

        return ResponseEntity.ok(new CurrentUserResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getMail(),
                utilisateur.getNumTel(),
                utilisateur.getRole(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null,
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getNom() : null
        ));
    }
}
