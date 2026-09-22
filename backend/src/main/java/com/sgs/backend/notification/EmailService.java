package com.sgs.backend.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * RG-07/RG-08 du cahier des charges : email automatique à la création
 * d'une commande client (confirmation) ou fournisseur (bon de commande).
 *
 * Pas de Thymeleaf : le pom.xml n'a que spring-boot-starter-mail, et deux
 * messages aussi courts ne justifient pas d'ajouter un moteur de template
 * (et sa configuration) rien que pour ça -- le HTML est construit ici,
 * inline. Si le besoin grandit (plus de templates, mise en page riche),
 * Thymeleaf reste l'upgrade naturel prévu par le cahier des charges (§6.2).
 *
 * Un échec d'envoi (SMTP non configuré -- MAIL_USERNAME/PASSWORD sont vides
 * par défaut dans application.yaml) est avalé et journalisé : l'envoi d'un
 * email ne doit jamais faire échouer la création d'une commande.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void envoyerConfirmationCommandeClient(String destinataire, String codeCommande, String nomClient) {
        envoyer(
                destinataire,
                "Confirmation de votre commande " + codeCommande,
                "<p>Bonjour " + nomClient + ",</p>"
                        + "<p>Votre commande <b>" + codeCommande + "</b> a bien été enregistrée.</p>"
                        + "<p>Merci de votre confiance.</p>"
        );
    }

    public void envoyerBonCommandeFournisseur(String destinataire, String codeCommande, String nomFournisseur) {
        envoyer(
                destinataire,
                "Bon de commande " + codeCommande,
                "<p>Bonjour " + nomFournisseur + ",</p>"
                        + "<p>Veuillez trouver ci-dessous notre bon de commande <b>" + codeCommande + "</b>.</p>"
        );
    }

    private void envoyer(String destinataire, String sujet, String corpsHtml) {
        if (destinataire == null || destinataire.isBlank()) {
            // Pas d'adresse renseignée : ce n'est pas une erreur, juste rien à envoyer.
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corpsHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Échec d'envoi d'email à {} : {}", destinataire, e.getMessage());
        }
    }
}
