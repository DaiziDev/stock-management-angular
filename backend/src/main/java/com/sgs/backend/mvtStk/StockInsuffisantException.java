package com.sgs.backend.mvtStk;

/**
 * Levée quand une SORTIE demanderait de faire passer le stock d'un article
 * sous zéro (RG-05 : le stock ne peut pas être négatif). Attrapée par
 * GlobalExceptionHandler -> 409 Conflict, jamais 500 : c'est un refus
 * métier attendu, pas un bug.
 *
 * Levée depuis une méthode @Transactional (MvtStkService.enregistrerMouvement),
 * ce qui annule TOUTE la transaction en cours -- c'est ce mécanisme qui
 * garantit qu'une vente ou une commande à plusieurs lignes est rejetée en
 * bloc si UNE SEULE ligne manque de stock (pas de mouvement partiel appliqué).
 */
public class StockInsuffisantException extends RuntimeException {
    public StockInsuffisantException(String message) {
        super(message);
    }
}
