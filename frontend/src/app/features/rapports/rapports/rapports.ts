import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RapportsService } from '../services/rapports-service';
import { AjustementStockModal } from '../ajustement-stock-modal/ajustement-stock-modal';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type ArticleStock, type Valorisation } from '../models/rapports.model';

/**
 * Page Rapports & stock : valorisation du stock, état article par article
 * (filtre articles en alerte) et ajustements d'inventaire via modal.
 * Le lien "Mouvements" en en-tête ouvre l'historique complet.
 */
@Component({
  selector: 'app-rapports',
  standalone: true,
  imports: [DecimalPipe, RouterLink, AppIcon, AjustementStockModal],
  templateUrl: './rapports.html',
})
export class Rapports implements OnInit {
  private readonly rapportsService = inject(RapportsService);

  readonly valorisation = signal<Valorisation | null>(null);
  readonly etatStock = signal<ArticleStock[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly recherche = signal('');
  readonly filtreAlerte = signal<'TOUS' | 'ALERTE' | 'RUPTURE'>('TOUS');

  readonly ajustementModal = viewChild.required(AjustementStockModal);

  /** Choix de filtres typés (le template ne peut pas typer un littéral inline). */
  readonly filtresAlerte: { v: 'TOUS' | 'ALERTE' | 'RUPTURE'; l: string }[] = [
    { v: 'TOUS', l: 'Tous' },
    { v: 'ALERTE', l: 'En alerte' },
    { v: 'RUPTURE', l: 'Ruptures' },
  ];

  readonly etatFiltre = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const filtre = this.filtreAlerte();
    return this.etatStock().filter((a) => {
      const enAlerte = a.seuilMin != null && a.stockActuel <= a.seuilMin;
      const okFiltre =
        filtre === 'TOUS' ||
        (filtre === 'ALERTE' && enAlerte) ||
        (filtre === 'RUPTURE' && a.stockActuel <= 0);
      const okRecherche = !q || a.designation.toLowerCase().includes(q) || a.codeArticle.toLowerCase().includes(q);
      return okFiltre && okRecherche;
    });
  });

  readonly nbAlertes = computed(
    () => this.etatStock().filter((a) => a.seuilMin != null && a.stockActuel <= a.seuilMin).length
  );
  readonly nbRuptures = computed(() => this.etatStock().filter((a) => a.stockActuel <= 0).length);

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    let restants = 2;
    const terminer = () => {
      if (--restants === 0) this.chargement.set(false);
    };
    this.rapportsService.valorisation().subscribe({
      next: (v) => {
        this.valorisation.set(v);
        terminer();
      },
      error: () => {
        this.erreur.set('Impossible de charger les données de stock. Le serveur est-il démarré ?');
        terminer();
      },
    });
    this.rapportsService.etat().subscribe({
      next: (liste) => {
        this.etatStock.set(liste);
        terminer();
      },
      error: () => terminer(),
    });
  }

  ouvrirAjustement(a: ArticleStock): void {
    this.ajustementModal().ouvrir(a);
  }

  /** Un article est en alerte si son stock atteint son seuil (seuil configuré). */
  enAlerte(a: ArticleStock): boolean {
    return a.seuilMin != null && a.stockActuel <= a.seuilMin;
  }

  /** Après ajustement : le stock et la valorisation ont changé. */
  surAjustementConfirme(_mvt?: unknown): void {
    this.rapportsService.etat().subscribe((liste) => this.etatStock.set(liste));
    this.rapportsService.valorisation().subscribe((v) => this.valorisation.set(v));
  }
}
