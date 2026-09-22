import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArticleService } from '../../articles/services/article-service';
import { ClientService } from '../../clients/services/client-service';
import { VenteService } from '../services/vente-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type Article } from '../../articles/models/article.model';

/**
 * Point de vente (POS) : grille de produits filtrable à gauche, panier à
 * droite. Une vente = POST /api/ventes unique ; le backend décrémente le
 * stock immédiatement et refuse EN BLOC (409) si une ligne dépasse le
 * stock disponible -- le composant affiche alors l'erreur sans perde le
 * panier, pour correction.
 */
@Component({
  selector: 'app-point-de-vente',
  standalone: true,
  imports: [FormsModule, AppIcon],
  templateUrl: './point-de-vente.html',
})
export class PointDeVente implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly clientService = inject(ClientService);
  /** protected : lu dans le template pour le ticket de confirmation. */
  protected readonly venteService = inject(VenteService);

  // ── Sources de données ──
  readonly articles = this.articleService.articles;
  readonly clients = this.clientService.clients;
  readonly chargement = signal(true);
  readonly erreurChargement = signal<string | null>(null);

  // ── Recherche / filtre catalogue ──
  readonly recherche = signal('');
  readonly articlesFiltres = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.articles();
    if (!q) return liste;
    return liste.filter(
      (a) =>
        a.designation.toLowerCase().includes(q) ||
        a.codeArticle.toLowerCase().includes(q) ||
        (a.categorie?.designation ?? '').toLowerCase().includes(q)
    );
  });

  // ── Panier (lignes de la vente en cours) ──
  readonly panier = signal<{ article: Article; quantite: number }[]>([]);
  readonly clientId = signal<number | null>(null);
  readonly encaissementEnCours = signal(false);
  readonly erreurEncaissement = signal<string | null>(null);
  readonly venteEnregistree = signal(false); // true → ticket de confirmation

  readonly total = computed(() =>
    this.panier().reduce((somme, l) => somme + Number(l.article.prixUnitaireTtc) * l.quantite, 0)
  );
  readonly nbArticles = computed(() => this.panier().reduce((n, l) => n + l.quantite, 0));

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
    this.erreurChargement.set('Impossible de charger le catalogue. Le serveur est-il démarré ?');
    terminer();
  }

  // ── Gestion du panier ──

  ajouter(article: Article): void {
    if (this.articleDejaAuPanier(article.id)) {
      this.incrementer(article.id);
      return;
    }
    this.panier.update((lignes) => [...lignes, { article, quantite: 1 }]);
    this.venteEnregistree.set(false);
  }

  incrementer(articleId: number): void {
    this.panier.update((lignes) =>
      lignes.map((l) => {
        if (l.article.id !== articleId) return l;
        const plafond = l.article.stockActuel ?? Number.MAX_SAFE_INTEGER;
        return { ...l, quantite: Math.min(l.quantite + 1, plafond) };
      })
    );
  }

  decrementer(articleId: number): void {
    this.panier.update((lignes) =>
      lignes.flatMap((l) => {
        if (l.article.id !== articleId) return [l];
        return l.quantite <= 1 ? [] : [{ ...l, quantite: l.quantite - 1 }];
      })
    );
  }

  retirer(articleId: number): void {
    this.panier.update((lignes) => lignes.filter((l) => l.article.id !== articleId));
  }

  vider(): void {
    this.panier.set([]);
    this.erreurEncaissement.set(null);
  }

  /** Stock restant affichable pour un article (hors quantités déjà au panier). */
  stockRestant(article: Article): number {
    const auPanier = this.panier().find((l) => l.article.id === article.id)?.quantite ?? 0;
    // stockActuel manquant = information non exposée : on ne bloque pas l'UI,
    // le backend reste le garant final du refus (409 en bloc).
    return (article.stockActuel ?? Number.MAX_SAFE_INTEGER) - auPanier;
  }

  private articleDejaAuPanier(articleId: number): boolean {
    return this.panier().some((l) => l.article.id === articleId);
  }

  // ── Encaissement ──

  encaisser(): void {
    if (this.panier().length === 0 || this.encaissementEnCours()) return;
    this.encaissementEnCours.set(true);
    this.erreurEncaissement.set(null);

    this.venteService
      .encaisser({
        clientId: this.clientId(),
        lignes: this.panier().map((l) => ({ articleId: l.article.id, quantite: l.quantite })),
      })
      .subscribe({
        next: () => {
          this.encaissementEnCours.set(false);
          this.panier.set([]);
          this.clientId.set(null);
          this.venteEnregistree.set(true);
          // Le stock a bougé côté backend : on resynchronise le catalogue.
          this.articleService.loadAll().subscribe();
        },
        error: (err) => {
          this.encaissementEnCours.set(false);
          const msg = (err?.error?.message as string) ?? null;
          this.erreurEncaissement.set(msg ?? 'Encaissement refusé : stock insuffisant ou erreur serveur.');
        },
      });
  }

  /** Conversion sûre d'une valeur de select (string) en id numérique. */
  toClientId(value: string | number | null): number | null {
    return value === null || value === '' ? null : Number(value);
  }

  /** Sous-total d'une ligne de panier (le template n'a pas accès à Number()). */
  sousTotal(ligne: { article: Article; quantite: number }): string {
    return this.montant(Number(ligne.article.prixUnitaireTtc) * ligne.quantite);
  }

  /** Stock plafond pour l'incrément (stockActuel optionnel dans le modèle). */
  stockMax(ligne: { article: Article; quantite: number }): number {
    return ligne.article.stockActuel ?? Number.MAX_SAFE_INTEGER;
  }

  montant(v: number | string | null | undefined): string {
    const n = Number(v ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }
}
