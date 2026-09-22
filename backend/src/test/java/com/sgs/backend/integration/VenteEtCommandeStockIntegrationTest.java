package com.sgs.backend.integration;

import com.sgs.backend.article.Article;
import com.sgs.backend.commandeClient.CommandeClient;
import com.sgs.backend.commandeClient.CommandeClientService;
import com.sgs.backend.commandeClient.dto.CommandeClientRequestDTO;
import com.sgs.backend.commandeClient.dto.LigneCommandeRequestDTO;
import com.sgs.backend.commandeFournisseur.CommandeFournisseur;
import com.sgs.backend.commandeFournisseur.CommandeFournisseurService;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.LigneCommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.ReceptionPartielleDTO;
import com.sgs.backend.mvtStk.StockInsuffisantException;
import com.sgs.backend.vente.VenteService;
import com.sgs.backend.vente.dto.LigneVenteRequestDTO;
import com.sgs.backend.vente.dto.VenteRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Règles bout-en-bout (métier + persistance réelle, sans rollback du test) :
 *   * RG-02 : valider une commande client génère une SORTIE par ligne ;
 *   * RG-03 : réceptionner une commande fournisseur génère une ENTRÉE par
 *     ligne, y compris en réception partielle (§3.5) ;
 *   * RG-04 : une vente décrémente le stock immédiatement, client ou comptoir ;
 *   * RG-05 : le stock jamais négatif — et le refus est EN BLOC grâce à
 *     l'annulation transactionnelle (le cœur du risque métier).
 */
class VenteEtCommandeStockIntegrationTest extends AbstractStockIntegrationTest {

    @Autowired
    private VenteService venteService;
    @Autowired
    private CommandeClientService commandeClientService;
    @Autowired
    private CommandeFournisseurService commandeFournisseurService;

    // ───────────── RG-04 : vente = sortie immédiate ─────────────

    @Test
    void venteDecrementeLeStockImmediatementAvecTrace_RG04() {
        Article a1 = creerArticle("Cahier", 50, null);
        Article a2 = creerArticle("Stylo", 30, null);

        venteService.create(new VenteRequestDTO(null, List.of(
                new LigneVenteRequestDTO(a1.getId(), 3),
                new LigneVenteRequestDTO(a2.getId(), 2)
        )));

        assertThat(stockDe(a1.getId())).isEqualTo(47);
        assertThat(stockDe(a2.getId())).isEqualTo(28);

        var mvtsA1 = mouvementsDe(a1.getId());
        assertThat(mvtsA1).hasSize(1);
        assertThat(mvtsA1.get(0).getType()).isEqualTo(com.sgs.backend.mvtStk.TypeMouvement.SORTIE);
        assertThat(mvtsA1.get(0).getOrigine()).startsWith("VT-");

        // La vente est bien persistée (pas seulement appliquée au stock).
        assertThat(venteRepository.findByEntrepriseIdOrderByDateVenteDesc(entreprise.getId())).hasSize(1);
    }

    @Test
    void venteAuComptoirSansClientEstPossible_RG04() {
        Article a1 = creerArticle("Article comptoir", 10, null);

        var vente = venteService.create(new VenteRequestDTO(null, List.of(
                new LigneVenteRequestDTO(a1.getId(), 4)
        )));

        assertThat(vente.clientNom()).isEqualTo("Client comptoir");
        assertThat(stockDe(a1.getId())).isEqualTo(6);
    }

    @Test
    void venteAvecClientIdentifie_RG04() {
        Article a1 = creerArticle("Article client", 10, null);
        var client = creerClient();

        var vente = venteService.create(new VenteRequestDTO(client.getId(), List.of(
                new LigneVenteRequestDTO(a1.getId(), 1)
        )));

        assertThat(vente.clientId()).isEqualTo(client.getId());
        assertThat(stockDe(a1.getId())).isEqualTo(9);
    }

    // ───────────── RG-05 : refus EN BLOC (rollback transactionnel) ─────────────

    @Test
    void venteRefuseeEnBlocSiUneSeuleLigneManque_RG05() {
        Article a1 = creerArticle("Article dispo", 50, null);
        Article a2 = creerArticle("Article sous-stocké", 2, null);

        // Ligne 1 OK (50 >= 10), ligne 2 impossible (2 < 5) : TOUT est refusé.
        assertThatThrownBy(() -> venteService.create(new VenteRequestDTO(null, List.of(
                new LigneVenteRequestDTO(a1.getId(), 10),
                new LigneVenteRequestDTO(a2.getId(), 5)
        )))).isInstanceOf(StockInsuffisantException.class);

        // Le cœur de la règle : la ligne 1 n'a PAS été décrémentée, aucun
        // mouvement n'a survécu, aucune vente n'est en base (rollback total).
        assertThat(stockDe(a1.getId())).isEqualTo(50);
        assertThat(stockDe(a2.getId())).isEqualTo(2);
        assertThat(mouvementsDe(a1.getId())).isEmpty();
        assertThat(mouvementsDe(a2.getId())).isEmpty();
        assertThat(venteRepository.findByEntrepriseIdOrderByDateVenteDesc(entreprise.getId())).isEmpty();
    }

    // ───────────── RG-02 : commande client -> sorties de stock ─────────────

    @Test
    void validerCommandeClientGenereUneSortieParLigne_RG02() {
        Article a1 = creerArticle("Commande client", 20, null);
        var client = creerClient();

        var commande = commandeClientService.create(new CommandeClientRequestDTO(client.getId(), List.of(
                new LigneCommandeRequestDTO(a1.getId(), 4)
        )));

        // Avant validation : aucun impact stock.
        assertThat(stockDe(a1.getId())).isEqualTo(20);

        var validee = commandeClientService.valider(commande.id());

        assertThat(validee.statut()).isEqualTo(com.sgs.backend.commande.StatutCommandeClient.VALIDEE);
        assertThat(stockDe(a1.getId())).isEqualTo(16);
        var mvts = mouvementsDe(a1.getId());
        assertThat(mvts).hasSize(1);
        assertThat(mvts.get(0).getType()).isEqualTo(com.sgs.backend.mvtStk.TypeMouvement.SORTIE);
        assertThat(mvts.get(0).getOrigine()).isEqualTo(commande.code());
    }

    @Test
    void validationRefuseeEnBlocSiStockInsuffisant_RG02_RG05() {
        Article a1 = creerArticle("Ligne OK", 100, null);
        Article a2 = creerArticle("Ligne KO", 1, null);
        var client = creerClient();

        var commande = commandeClientService.create(new CommandeClientRequestDTO(client.getId(), List.of(
                new LigneCommandeRequestDTO(a1.getId(), 5),
                new LigneCommandeRequestDTO(a2.getId(), 3)
        )));

        assertThatThrownBy(() -> commandeClientService.valider(commande.id()))
                .isInstanceOf(StockInsuffisantException.class);

        // Rollback : la ligne 1 n'a pas bougé, la commande reste EN_COURS.
        assertThat(stockDe(a1.getId())).isEqualTo(100);
        assertThat(stockDe(a2.getId())).isEqualTo(1);
        CommandeClient enBase = commandeClientRepository.findById(commande.id()).orElseThrow();
        assertThat(enBase.getStatut()).isEqualTo(com.sgs.backend.commande.StatutCommandeClient.EN_COURS);
        assertThat(mouvementsDe(a1.getId())).isEmpty();
    }

    @Test
    void annulerUneCommandeEnCoursNeTouchePasAuStock_RG02() {
        Article a1 = creerArticle("Commande annulée", 15, null);
        var client = creerClient();

        var commande = commandeClientService.create(new CommandeClientRequestDTO(client.getId(), List.of(
                new LigneCommandeRequestDTO(a1.getId(), 6)
        )));

        commandeClientService.annuler(commande.id());

        CommandeClient enBase = commandeClientRepository.findById(commande.id()).orElseThrow();
        assertThat(enBase.getStatut()).isEqualTo(com.sgs.backend.commande.StatutCommandeClient.ANNULEE);
        assertThat(stockDe(a1.getId())).isEqualTo(15);
        assertThat(mouvementsDe(a1.getId())).isEmpty();
    }

    // ───────────── RG-03 : commande fournisseur -> entrées de stock ─────────────

    @Test
    void receptionCompleteGenereUneEntreeParLigne_RG03() {
        Article a1 = creerArticle("Réception", 0, null);
        var fournisseur = creerFournisseur();

        var commande = commandeFournisseurService.create(new CommandeFournisseurRequestDTO(fournisseur.getId(), List.of(
                new LigneCommandeFournisseurRequestDTO(a1.getId(), 7)
        )));

        var recue = commandeFournisseurService.receptionner(commande.id());

        assertThat(recue.statut()).isEqualTo(com.sgs.backend.commande.StatutCommandeFournisseur.RECUE);
        assertThat(stockDe(a1.getId())).isEqualTo(7);
        var mvts = mouvementsDe(a1.getId());
        assertThat(mvts).hasSize(1);
        assertThat(mvts.get(0).getType()).isEqualTo(com.sgs.backend.mvtStk.TypeMouvement.ENTREE);
        assertThat(mvts.get(0).getOrigine()).isEqualTo(commande.code());

        // Anti double-réception : le stock serait compté deux fois.
        assertThatThrownBy(() -> commandeFournisseurService.receptionner(commande.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà reçue");
        assertThat(stockDe(a1.getId())).isEqualTo(7);
    }

    @Test
    void receptionPartielleEntreLesStocksParEtapes_RG03() {
        Article a1 = creerArticle("Livraison partielle A", 0, null);
        Article a2 = creerArticle("Livraison partielle B", 0, null);
        var fournisseur = creerFournisseur();

        var commande = commandeFournisseurService.create(new CommandeFournisseurRequestDTO(fournisseur.getId(), List.of(
                new LigneCommandeFournisseurRequestDTO(a1.getId(), 10),
                new LigneCommandeFournisseurRequestDTO(a2.getId(), 5)
        )));
        Long ligne1 = commande.lignes().get(0).id();
        Long ligne2 = commande.lignes().get(1).id();

        // 1re réception partielle : 4 unités sur la ligne 1 seulement.
        var partielle = commandeFournisseurService.receptionnerPartiellement(commande.id(),
                new ReceptionPartielleDTO(List.of(new ReceptionPartielleDTO.LigneRecueDTO(ligne1, 4))));

        assertThat(partielle.statut()).isEqualTo(com.sgs.backend.commande.StatutCommandeFournisseur.RECUE_PARTIELLEMENT);
        assertThat(stockDe(a1.getId())).isEqualTo(4);
        assertThat(stockDe(a2.getId())).isEqualTo(0);

        // 2e réception : le solde des deux lignes -> bascule automatique en RECUE.
        var completee = commandeFournisseurService.receptionnerPartiellement(commande.id(),
                new ReceptionPartielleDTO(List.of(
                        new ReceptionPartielleDTO.LigneRecueDTO(ligne1, 6),
                        new ReceptionPartielleDTO.LigneRecueDTO(ligne2, 5)
                )));

        assertThat(completee.statut()).isEqualTo(com.sgs.backend.commande.StatutCommandeFournisseur.RECUE);
        assertThat(stockDe(a1.getId())).isEqualTo(10);
        assertThat(stockDe(a2.getId())).isEqualTo(5);

        // Les quantités reçues sont persistées ligne par ligne (lues dans
        // le DTO renvoyé par le service, la collection JPA étant lazy hors
        // transaction de test).
        // L'ordre des lignes d'une collection JPA n'est pas garanti (pas de
        // @OrderBy) : on assère sans ordre.
        assertThat(completee.lignes()).extracting("quantiteRecue").containsExactlyInAnyOrder(10, 5);
        assertThat(mouvementsDe(a1.getId())).hasSize(2); // 4 puis 6
    }

    @Test
    void receptionPartielleRefusePlusQueLeSoldeRestant_RG03() {
        Article a1 = creerArticle("Dépassement", 0, null);
        var fournisseur = creerFournisseur();

        var commande = commandeFournisseurService.create(new CommandeFournisseurRequestDTO(fournisseur.getId(), List.of(
                new LigneCommandeFournisseurRequestDTO(a1.getId(), 10)
        )));
        Long ligne1 = commande.lignes().get(0).id();

        assertThatThrownBy(() -> commandeFournisseurService.receptionnerPartiellement(commande.id(),
                new ReceptionPartielleDTO(List.of(new ReceptionPartielleDTO.LigneRecueDTO(ligne1, 11)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restant");

        assertThat(stockDe(a1.getId())).isEqualTo(0);
        CommandeFournisseur enBase = commandeFournisseurRepository.findById(commande.id()).orElseThrow();
        assertThat(enBase.getStatut()).isEqualTo(com.sgs.backend.commande.StatutCommandeFournisseur.EN_ATTENTE);
    }

    @Test
    void annulationRefuseeApresReceptionPartielle_RG03() {
        Article a1 = creerArticle("Annulation partielle", 0, null);
        var fournisseur = creerFournisseur();

        var commande = commandeFournisseurService.create(new CommandeFournisseurRequestDTO(fournisseur.getId(), List.of(
                new LigneCommandeFournisseurRequestDTO(a1.getId(), 10)
        )));
        Long ligne1 = commande.lignes().get(0).id();

        commandeFournisseurService.receptionnerPartiellement(commande.id(),
                new ReceptionPartielleDTO(List.of(new ReceptionPartielleDTO.LigneRecueDTO(ligne1, 3))));

        // Le stock a déjà bougé (3 entrées) : annulation interdite.
        assertThatThrownBy(() -> commandeFournisseurService.annuler(commande.id()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stockDe(a1.getId())).isEqualTo(3);
    }
}
