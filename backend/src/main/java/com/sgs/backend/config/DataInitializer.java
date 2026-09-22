package com.sgs.backend.config;

import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;

import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Exécuté au démarrage de l'application.
 *
 * Crée le compte SUPER_ADMIN "bootstrap" — l'opérateur de la plateforme.
 * C'est lui qui onboardera les entreprises clientes depuis la console
 * plateforme (créer l'entreprise, puis son premier ADMIN). Il n'est
 * rattaché à AUCUNE entreprise : son JWT porte entrepriseId=null, ce qui
 * le distingue structurellement des comptes clients.
 *
 * C'est la réponse au problème soulevé par le flux logique fonctionnel :
 * "le tout premier ADMIN du système ne peut pas être créé par un ADMIN
 * déjà connecté" → tranché par un compte inséré au démarrage.
 *
 * Le mot de passe est hashé dynamiquement avec BCrypt — pas besoin de hash
 * pré-généré en dur dans le SQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String BOOTSTRAP_LOGIN = "admin@sgs.local";

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        upgraderBootstrapEnSuperAdmin();
        creerEntrepriseDemo();
    }

    /**
     * Crée le compte bootstrap s'il n'existe pas, ou le MET À NIVEAU s'il
     * provient d'une base créée avant l'introduction du rôle SUPER_ADMIN
     * (il était alors ADMIN rattaché à l'entreprise de démo — ce qui aurait
     * fini par le traiter comme un simple tenant). Idempotent : relancer
     * l'app ne modifie rien.
     */
    private void upgraderBootstrapEnSuperAdmin() {
        Utilisateur bootstrap = utilisateurRepository.findByLogin(BOOTSTRAP_LOGIN).orElseGet(() -> {
            Utilisateur nouveau = new Utilisateur();
            nouveau.setNom("Plateforme");
            nouveau.setPrenom("Admin");
            nouveau.setLogin(BOOTSTRAP_LOGIN);
            nouveau.setMotDePasse(passwordEncoder.encode("admin123"));
            nouveau.setMail(BOOTSTRAP_LOGIN);
            nouveau.setNumTel("+237 6 00 00 00 00");
            nouveau.setRole(UserRole.SUPER_ADMIN);
            nouveau.setEntreprise(null);
            utilisateurRepository.save(nouveau);
            log.info("Compte SUPER_ADMIN bootstrap créé — login: {} | mdp: admin123", BOOTSTRAP_LOGIN);
            return nouveau;
        });

        if (bootstrap.getRole() != UserRole.SUPER_ADMIN || bootstrap.getEntreprise() != null) {
            bootstrap.setRole(UserRole.SUPER_ADMIN);
            bootstrap.setEntreprise(null);
            utilisateurRepository.save(bootstrap);
            log.info("Compte {} mis à niveau vers SUPER_ADMIN (rôle/entreprise corrigés)", BOOTSTRAP_LOGIN);
        } else {
            log.info("ℹ Compte bootstrap déjà à niveau, pas de modification");
        }
    }

    /**
     * Entreprise de démonstration pour tester le côté "tenant". Ce n'est PAS
     * l'entreprise de la plateforme : le SUPER_ADMIN n'y est plus rattaché,
     * elle est désormais une cliente comme les autres.
     */
    private void creerEntrepriseDemo() {
        boolean existe = entrepriseRepository.findAll().stream()
                .anyMatch(e -> "SGS Demo".equals(e.getNom()));
        if (!existe) {
            Entreprise demo = new Entreprise();
            demo.setNom("SGS Demo");
            demo.setMail("contact@sgs-demo.local");
            demo.setNumTel("+237 6 00 00 00 00");
            entrepriseRepository.save(demo);
            log.info("Entreprise de démonstration créée : SGS Demo");
        }
    }
}
