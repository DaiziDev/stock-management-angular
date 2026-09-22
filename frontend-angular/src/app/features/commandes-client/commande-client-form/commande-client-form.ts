import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ArticleService } from '../../articles/services/article-service';
import { ClientService } from '../../clients/services/client-service';
import { CommandeClientService } from '../services/commande-client-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type Article } from '../../articles/models/article.model';
import { type Client } from '../../clients/models/client.model';

/**
 * Création d'une commande client : client obligatoire + lignes
 * (article + quantité). Contrairement au POS, la commande ne touche PAS
 * au stock à la création — la validation (bouton "Valider" de la liste)
 * génèrera les sorties. Le stock disponible n'est donc qu'un avertissement
 * ici, pas un plafond bloquant (on commande ce que le client demande).
 */
@Component({
  selector: 'app-commande-client-form',
  standalone: true,
  imports: [FormsModule, RouterLink, AppIcon],
  templateUrl: './commande-client-form.html',
})
export class CommandeClientForm implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly clientService = inject(ClientService);
  private readonly commandeService = inject(CommandeClientService);
  private readonly router = inject(Router);

  readonly articles = this.articleService.articles;
  readonly clients = this.clientService.clients;
  readonly chargement = signal(true);
  readonly erreurChargement = signal<string | null>(null);

  readonly clientId = signal<number | null>(null);
  readonly lignes = signal<{ article: Article; quantite: number }[]>([]);

  readonly rechercheArticle = signal('');
  readonly articlesFiltres = computed(() => {
    const q = this.rechercheArticle().trim().toLowerCase();
    const liste = this.articles();
    if (!q) return liste;
    return liste.filter(
      (a) =>
        a.designation.toLowerCase().includes(q) ||
        a.codeArticle.toLowerCase().includes(q) ||
        (a.categorie?.designation ?? '').toLowerCase().includes(q)
    );
  });

  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  readonly total = computed(() =>
    this.lignes().reduce((somme, l) => somme + Number(l.article.prixUnitaireTtc) * l.quantite, 0)
  );
  readonly nbUnites = computed(() => this.lignes().reduce((n, l) => n + l.quantite, 0));

  /** Alerte si une ligne dépasse le stock actuel (la vente refusera sinon). */
  readonly lignesSousStock = computed(
    () => this.lignes().filter((l) => (l.article.stockActuel ?? 0) < l.quantite).length
  );

  ngOnInit(): void {
    this.chargement.set(true);
    let restants = 2;
    const terminer = () => {
      if (--restants === 0) this.chargement.set(false);
    };
    this.articleService.loadAll().subscribe({ next: terminer, error: () => this.echecChargement(terminer) });
    this.clientService.loadAll().subscribe({ next: terminer, error: () => this.echecChargement(terminer) });
  }

  private echecChargement(terminer: () => void): void {
    this.erreurChargement.set('Impossible de charger le catalogue et les clients. Le serveur est-il démarré ?');
    terminer();
  }

  ajouterArticle(article: Article): void {
    if (this.lignes().some((l) => l.article.id === article.id)) return;
    this.lignes.update((lignes) => [...lignes, { article, quantite: 1 }]);
  }

  changerQuantite(articleId: number, valeur: string): void {
    const q = Math.max(1, Math.floor(Number(valeur) || 1));
    this.lignes.update((lignes) => lignes.map((l) => (l.article.id === articleId ? { ...l, quantite: q } : l)));
  }

  incrementer(articleId: number): void {
    this.lignes.update((lignes) =>
      lignes.map((l) => (l.article.id === articleId ? { ...l, quantite: l.quantite + 1 } : l))
    );
  }

  decrementer(articleId: number): void {
    this.lignes.update((lignes) =>
      lignes.flatMap((l) => (l.article.id === articleId && l.quantite > 1 ? [{ ...l, quantite: l.quantite - 1 }] : l.article.id === articleId ? [] : [l]))
    );
  }

  retirer(articleId: number): void {
    this.lignes.update((lignes) => lignes.filter((l) => l.article.id !== articleId));
  }

  enregistrer(): void {
    if (!this.clientId() || this.lignes().length === 0 || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    this.commandeService
      .creer({
        clientId: this.clientId()!,
        lignes: this.lignes().map((l) => ({ articleId: l.article.id, quantite: l.quantite })),
      })
      .subscribe({
        next: () => this.router.navigate(['/commandes-client']),
        error: (err) => {
          this.envoiEnCours.set(false);
          const msg = (err?.error?.message as string) ?? null;
          this.erreur.set(msg ?? 'Création impossible. Vérifiez les lignes puis réessayez.');
        },
      });
  }

  toClientId(value: string | number | null): number | null {
    return value === null || value === '' ? null : Number(value);
  }

  sousTotal(ligne: { article: Article; quantite: number }): string {
    return this.montant(Number(ligne.article.prixUnitaireTtc) * ligne.quantite);
  }

  montant(v: number | string | null | undefined): string {
    const n = Number(v ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }
}
