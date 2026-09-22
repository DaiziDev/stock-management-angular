import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlateformeService } from '../services/plateforme-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type PlateformeStats } from '../models/plateforme.models';

/**
 * Vue d'ensemble de la console plateforme : santé du PARC (entreprises,
 * comptes), activité agrégée (ventes, CA du mois, alertes stock) et
 * dernières entreprises onboardées. C'est le pendant "opérateur" du
 * dashboard d'entreprise -- volontairement différent : ici on pilote le
 * parc, pas le stock d'une boutique.
 */
@Component({
  selector: 'app-plateforme-dashboard',
  imports: [DecimalPipe, RouterLink, AppIcon],
  templateUrl: './plateforme-dashboard.html',
})
export class PlateformeDashboard implements OnInit {
  private readonly plateformeService = inject(PlateformeService);

  readonly stats = signal<PlateformeStats | null>(null);
  readonly chargement = signal(true);

  ngOnInit(): void {
    this.plateformeService.stats().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  /** "1250000.00" (string JSON d'un BigDecimal) → "1 250 000" FCFA. */
  formatCA(ca: string): string {
    const n = Number(ca ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }

  /** "2026-09-14T10:22:31.082" → "14 sept. 2026". */
  formatDate(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime())
      ? '—'
      : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
