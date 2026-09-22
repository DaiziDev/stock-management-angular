import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { RapportsService } from '../services/rapports-service';
import { AjustementStockModal } from '../ajustement-stock-modal/ajustement-stock-modal';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type ArticleStock, type MvtStk, type TypeMouvement } from '../models/rapports.model';

/**
 * Historique des mouvements de stock (entrées, sorties, ajustements) :
 * filtres par type et par article, traçabilité complète (origine métier,
 * motif, stock après mouvement). Ajustement d'inventaire possible depuis
 * ici aussi.
 */
@Component({
  selector: 'app-mouvements-stock',
  standalone: true,
  imports: [AppIcon, AjustementStockModal],
  templateUrl: './mouvements-stock.html',
})
export class MouvementsStock implements OnInit {
  private readonly rapportsService = inject(RapportsService);

  readonly mouvementsListe = signal<MvtStk[]>([]);
  readonly articlesPourFiltre = signal<ArticleStock[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly filtreType = signal<'TOUS' | TypeMouvement>('TOUS');
  readonly filtreArticleId = signal<number | null>(null);

  readonly ajustementModal = viewChild.required(AjustementStockModal);

  /** Choix de filtres typés (le template ne peut pas typer un littéral inline). */
  readonly filtres: { v: 'TOUS' | TypeMouvement; l: string }[] = [
    { v: 'TOUS', l: 'Tous' },
    { v: 'ENTREE', l: 'Entrées' },
    { v: 'SORTIE', l: 'Sorties' },
    { v: 'AJUSTEMENT', l: 'Ajustements' },
  ];

  readonly mouvementsFiltres = computed(() => {
    const type = this.filtreType();
    const articleId = this.filtreArticleId();
    return this.mouvementsListe().filter((m) => {
      const okType = type === 'TOUS' || m.type === type;
      const okArticle = articleId === null || m.articleId === articleId;
      return okType && okArticle;
    });
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.rapportsService.mouvements().subscribe({
      next: (liste) => {
        this.mouvementsListe.set(liste);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les mouvements. Le serveur est-il démarré ?');
        this.chargement.set(false);
      },
    });
  }

  ouvrirAjustement(): void {
    const premier = this.mouvementsListe()[0];
    const articleFiltre = this.articlesPourFiltre().find((a) => a.articleId === this.filtreArticleId());
    const cible = articleFiltre ?? (premier ? { articleId: premier.articleId, codeArticle: '', designation: '', stockActuel: premier.stockActuelApres, seuilMin: null } : null);
    if (cible) this.ajustementModal().ouvrir(cible);
  }

  surAjustementConfirme(_mvt?: unknown): void {
    this.charger();
    this.rapportsService.etat().subscribe((liste) => this.articlesPourFiltre.set(liste));
  }

  changerFiltreArticle(valeur: string): void {
    this.filtreArticleId.set(valeur === '' ? null : Number(valeur));
  }

  libelleType(type: TypeMouvement): string {
    return type === 'ENTREE' ? 'Entrée' : type === 'SORTIE' ? 'Sortie' : 'Ajustement';
  }

  /** Couleurs du badge de type — tokens du design system (dark mode OK). */
  badgeType(type: TypeMouvement): string {
    return type === 'ENTREE'
      ? 'bg-success-bg text-success'
      : type === 'SORTIE'
        ? 'bg-danger-bg text-danger'
        : 'bg-info-bg text-info';
  }

  /** Quantité signée affichée : +5 en entrée, -3 en sortie/ajustement négatif. */
  quantiteAffichee(m: MvtStk): string {
    return m.quantite >= 0 ? `+${m.quantite}` : `${m.quantite}`;
  }

  dateCourte(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  heure(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
