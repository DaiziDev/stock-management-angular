import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../services/dashboard-service';
import { ThemeService } from '../../../core/services/theme.service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { AppChart } from '../../../shared/components/chart/chart';
import { chartTheme, chartTooltip } from '../../../shared/components/chart/chart-theme';
import type { ChartConfiguration } from 'chart.js';
import {
  type ArticleStockDTO,
  type DashboardKpis,
  type GraphiquesResponse,
  type VenteListe,
} from '../models/dashboard.models';

/**
 * Tableau de bord d'ENTREPRISE (ADMIN, GESTIONNAIRE, VENDEUR) : KPIs métier
 * (stock, commandes, CA du mois), activité des 7 derniers jours et listes
 * d'action (alertes de stock, dernières ventes).
 *
 * Données réelles du tenant, agrégées côté backend (/api/dashboard/kpis) et
 * complétées par les endpoints stock/ventes -- rien de codé en dur.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DecimalPipe, RouterLink, AppIcon, AppChart],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly themeService = inject(ThemeService);

  readonly kpis = signal<DashboardKpis | null>(null);
  readonly alertes = signal<ArticleStockDTO[]>([]);
  readonly ventes = signal<VenteListe[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal(false);

  /** Données brutes des graphiques (null = pas encore chargé / échec). */
  readonly graphiques = signal<GraphiquesResponse | null>(null);

  /** CA par jour des 7 derniers jours (index 0 = il y a 6 jours ... 6 = aujourd'hui). */
  readonly serieCA = signal<number[]>(Array(7).fill(0));

  /** Somme des 7 jours (sous-titre du graphique). */
  readonly totalSemaine = computed(() => this.serieCA().reduce((a, b) => a + b, 0));

  /** Étiquettes du graphique : initiales des jours (L M M J V S D). */
  readonly joursSemaine = computed(() => {
    const fmt = new Intl.DateTimeFormat('fr-FR', { weekday: 'narrow' });
    const jours: string[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      jours.push(fmt.format(d));
    }
    return jours;
  });

  ngOnInit(): void {
    this.dashboardService.chargerTout().subscribe({
      next: ({ kpis, alertes, ventes }) => {
        this.kpis.set(kpis);
        this.alertes.set(alertes);
        this.ventes.set(ventes);
        this.serieCA.set(this.dashboardService.calculerSerieCA(ventes));
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set(true);
      },
    });

    // Graphiques Chart.js : chargé en parallèle du reste, échec non bloquant
    // (le service tombe sur des séries vides).
    this.dashboardService.chargerGraphiques().subscribe((g) => this.graphiques.set(g));
  }

  heure(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime())
      ? '—'
      : d.toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  /** Hauteur (en %) de la barre du jour i pour le graphique CA en barres. */
  hauteurBarre(i: number): number {
    const serie = this.serieCA();
    const max = Math.max(...serie, 1);
    return Math.max((serie[i] / max) * 100, serie[i] > 0 ? 6 : 2.5);
  }

  montant(v: number | string | null | undefined): string {
    const n = Number(v ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }

  // ─────────── Graphiques Chart.js ───────────

  /** L'évolution entrées/sorties est prête (au moins un point reçu). */
  readonly aEvolution = computed(() => (this.graphiques()?.evolutionStock.length ?? 0) > 0);

  /** La série du top articles vendus est prête. */
  readonly aTopArticles = computed(() => (this.graphiques()?.topArticles.length ?? 0) > 0);

  /**
   * Graphique en aires : évolution des entrées et sorties de stock sur
   * 30 jours (cahier des charges §3.9). Recoloré au changement de thème
   * via AppChart (les computed dépendent des signals → nouvelle config à
   * chaque bascule, re-resolvant les tokens CSS).
   */
  readonly configEvolutionStock = computed<ChartConfiguration>(() => {
    const g = this.graphiques();
    // Dépendance au signal de thème : sans cette lecture, la computed ne se
    // recalculerait PAS au basculement clair/sombre (chartTheme() lit des
    // tokens CSS, ce n'est pas un signal) et AppChart ne se recolorerait pas.
    this.themeService.isDark();
    const t = chartTheme();
    const labels = this.dashboardService.labelsJours(g?.evolutionStock ?? []);
    const entrees = (g?.evolutionStock ?? []).map((j) => j.entrees);
    const sorties = (g?.evolutionStock ?? []).map((j) => j.sorties);

    return {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Entrées',
            data: entrees,
            borderColor: t.success,
            backgroundColor: hexAlpha(t.success, 0.14),
            fill: true,
            tension: 0.35,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4,
          },
          {
            label: 'Sorties',
            data: sorties,
            borderColor: t.danger,
            backgroundColor: hexAlpha(t.danger, 0.14),
            fill: true,
            tension: 0.35,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: {
            labels: { color: t.textSecondary, boxWidth: 8, boxHeight: 8, usePointStyle: true, pointStyle: 'circle' },
          },
          tooltip: chartTooltip(t),
        },
        scales: {
          x: {
            ticks: { color: t.textSecondary, maxTicksLimit: 10, font: { size: 10 } },
            grid: { display: false },
            border: { color: t.line },
          },
          y: {
            beginAtZero: true,
            ticks: { color: t.textSecondary, precision: 0, font: { size: 10 } },
            grid: { color: t.line },
            border: { display: false },
          },
        },
      },
    };
  });

  /**
   * Graphique en barres horizontales : top 5 des articles les plus vendus
   * sur 30 jours (roadmap #13). Les libellés sont tronqués pour rester
   * lisibles sur une échelle category.
   */
  readonly configTopArticles = computed<ChartConfiguration>(() => {
    const g = this.graphiques();
    this.themeService.isDark(); // dépendance de thème (voir configEvolutionStock)
    const t = chartTheme();
    const top = g?.topArticles ?? [];

    return {
      type: 'bar',
      data: {
        labels: top.map((a) => tronquer(a.designation, 22)),
        datasets: [
          {
            label: 'Quantité vendue',
            data: top.map((a) => a.quantiteVendue),
            backgroundColor: t.gold,
            borderRadius: 5,
            maxBarThickness: 22,
          },
        ],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            ...chartTooltip(t),
            callbacks: {
              afterLabel: (item) => {
                const a = top[item.dataIndex];
                return a ? `CA : ${Number(a.chiffreAffaires).toLocaleString('fr-FR')} FCFA` : '';
              },
            },
            displayColors: false,
          },
        },
        scales: {
          x: {
            beginAtZero: true,
            ticks: { color: t.textSecondary, precision: 0, font: { size: 10 } },
            grid: { color: t.line },
            border: { display: false },
          },
          y: {
            ticks: { color: t.textPrimary, font: { size: 11 } },
            grid: { display: false },
            border: { color: t.line },
          },
        },
      },
    };
  });
}

/** Tronque un libellé trop long pour l'axe Y du top articles. */
function tronquer(s: string, max: number): string {
  return s.length > max ? s.slice(0, max - 1) + '…' : s;
}

/** Convertit "#rrggbb" en "rgba(r, g, b, alpha)" (aires sous les courbes). */
function hexAlpha(hex: string, alpha: number): string {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m) return hex;
  const n = parseInt(m[1], 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
}
