package com.sgs.backend.utilisateur;

import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.utilisateur.dto.UtilisateurUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service Utilisateur — implémente UserDetailsService pour s'intégrer
 * nativement avec Spring Security.
 *
 * Spring Security utilise cette classe pour :
 *   - Charger un utilisateur par son login lors de l'authentification
 *   - Comparer le mot de passe hashé
 *   - Attribuer les rôles/permissions
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Appelé par Spring Security (viaJwtAuthFilter) pour charger un utilisateur
     * par son identifiant (login/email).
     *
     * @param login le login de l'utilisateur
     * @return un objet UserDetails que Spring Security utilise pour vérifier
     *         le mot de passe et les rôles
     * @throws UsernameNotFoundException si aucun utilisateur ne correspond
     */
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé avec le login : " + login
                ));

        // Construire les authorities (rôles) au format Spring Security
        // Le préfixe "ROLE_" est une convention Spring Security pour les rôles
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())
        );

        // Retourner un objet User Spring Security avec :
        //   - login comme username
        //   - motDePasse hashé
        //   - rôles
        //   - true = le compte est actif
        //   - true = le compte n'est pas expiré
        //   - true = le mot de passe n'est pas expiré
        //   - true = le compte n'est pas verrouillé
        return new User(
                utilisateur.getLogin(),
                utilisateur.getMotDePasse(),
                true, true, true, true,
                authorities
        );
    }

    /**
     * Recherche un utilisateur par son login (pour le controller auth).
     */
    public Utilisateur findByLogin(String login) {
        return utilisateurRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec le login : " + login
                ));
    }

    /**
     * Vérifie si un login existe déjà.
     */
    public boolean existsByLogin(String login) {
        return utilisateurRepository.existsByLogin(login);
    }

    /**
     * Crée un nouvel utilisateur avec le mot de passe hashé.
     */
    public Utilisateur create(Utilisateur utilisateur) {
        if (existsByLogin(utilisateur.getLogin())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec le login '" + utilisateur.getLogin() + "' existe déjà"
            );
        }
        // Hasher le mot de passe avant de sauvegarder
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Utilisé par UtilisateurController. Pas de CurrentUserService ici
     * (dépendance circulaire : CurrentUserService dépend déjà de ce
     * service pour résoudre "qui est connecté") -- le controller passe
     * directement l'entrepriseId de l'appelant.
     */
    public List<Utilisateur> findAllByEntreprise(Long entrepriseId) {
        return utilisateurRepository.findByEntrepriseId(entrepriseId);
    }

    public Utilisateur findByIdAndEntreprise(Long id, Long entrepriseId) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id=" + id));
        boolean appartientAuTenant = utilisateur.getEntreprise() != null
                && utilisateur.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec id=" + id);
        }
        return utilisateur;
    }

    public Utilisateur update(Long id, Long entrepriseId, UtilisateurUpdateDTO dto) {
        Utilisateur utilisateur = findByIdAndEntreprise(id, entrepriseId);
        utilisateur.setNom(dto.nom());
        utilisateur.setPrenom(dto.prenom());
        utilisateur.setMail(dto.mail());
        utilisateur.setNumTel(dto.numTel());
        utilisateur.setRole(dto.role());
        return utilisateurRepository.save(utilisateur);
    }

    public void delete(Long id, Long entrepriseId) {
        Utilisateur utilisateur = findByIdAndEntreprise(id, entrepriseId);
        utilisateurRepository.delete(utilisateur);
    }
}
