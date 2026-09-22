package com.sgs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre JWT — intercepte CHAQUE requête HTTP entrante.
 *
 * Flow :
 *   1. Lit le header "Authorization: Bearer <token>"
 *   2. Extrait l'email du token
 *   3. Charge l'utilisateur depuis la BDD via UserDetailsService
 *   4. Valide le token (signature + date d'expiration)
 *   5. Si tout est OK → met l'utilisateur dans le SecurityContext
 *      → les controllers peuvent ensuite utiliser @AuthenticationPrincipal
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lire le header Authorization
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → on laisse passer la requête
            // (Spring Security décidera si elle est autorisée ou non)
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraire le token (enlever "Bearer " au début)
        final String jwt = authHeader.substring(7);

        try {
            // 3. Extraire l'email du token
            final String email = jwtUtil.extractEmail(jwt);

            // 4. Si on a un email et que le SecurityContext est vide
            //    (l'utilisateur n'est pas encore authentifié dans cette requête)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Charger l'utilisateur depuis la BDD
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 6. Valider le token (signature correcte + non expiré)
                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // 7. Extraire le rôle depuis le token
                    String role = jwtUtil.extractRole(jwt);
                    List<SimpleGrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : List.of();

                    // 8. Créer l'objet d'authentification Spring Security
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    authorities
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 9. Poser l'authentification dans le SecurityContext
                    //    → tous les controllers/filters suivants peuvent y accéder
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalide ou erreur d'extraction → on ne met rien dans le SecurityContext
            // La requête continuera sans authentification
            logger.warn("JWT invalide : " + e.getMessage());
        }

        // 10. Passer au filtre suivant dans la chaîne
        filterChain.doFilter(request, response);
    }
}
