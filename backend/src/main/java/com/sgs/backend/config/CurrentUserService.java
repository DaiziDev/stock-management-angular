package com.sgs.backend.config;

import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Point d'accès unique à "qui est connecté" et "pour quelle entreprise".
 *
 * Le filtrage multi-tenant (RG-10 du cahier des charges : les données d'une
 * entreprise ne sont jamais visibles par une autre) passe TOUJOURS par ici —
 * jamais par un entrepriseId envoyé par le client dans une requête. On le
 * déduit uniquement du JWT de la requête en cours (posé dans le
 * SecurityContext par JwtAuthFilter).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UtilisateurService utilisateurService;

    public Utilisateur getUtilisateurCourant() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurService.findByLogin(login);
    }

    public Entreprise getEntrepriseCourante() {
        return getUtilisateurCourant().getEntreprise();
    }

    public Long getEntrepriseId() {
        Entreprise entreprise = getEntrepriseCourante();
        return entreprise != null ? entreprise.getId() : null;
    }
}
