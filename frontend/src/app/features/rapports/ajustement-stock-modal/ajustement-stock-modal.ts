import { Component, computed, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RapportsService } from '../services/rapports-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type ArticleStock, type AjustementRequest, type MvtStk } from '../models/rapports.model';

/**
 * Modal d'ajustement de stock (correction d'inventaire, RG-06) :
 * article + quantité signée (+/-) + motif obligatoire. Réutilisé par les
 * pages Rapports et Mouvements de stock. Émet l'événement `confirme` avec
 * le mouvement renvoyé par le backend pour que l'écran appelant
 * resynchronise ses données.
 */
@Component({
  selector: 'app-ajustement-stock-modal',
  standalone: true,
  imports: [FormsModule, AppIcon],
  template: `
    @if (ouvert() && article(); as a) {
      <div class="fixed inset-0 z-[60] flex items-center justify-center bg-ink-950/45 p-4 backdrop-blur-[2px]" (click)="fermer()">
        <div class="w-full max-w-md rounded-lg bg-surface p-6 shadow-lg max-sm:p-4" (click)="$event.stopPropagation()">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div>
              <b class="block font-display text-lg font-[650]">Ajustement de stock</b>
              <span class="text-xs text-gray-400">Correction d'inventaire tracée — le motif est obligatoire.</span>
            </div>
            <button type="button" class="rounded-md p-2 text-gray-400 hover:bg-surface-alt hover:text-ink-800" (click)="fermer()" aria-label="Fermer">
              <app-icon name="x" [size]="16" />
            </button>
          </div>

          <!-- Article concerné -->
          <div class="mb-4 flex items-center gap-3 rounded-md border border-line bg-surface-alt p-3">
            <span class="flex h-9 w-9 items-center justify-center rounded-md bg-surface font-mono text-2xs font-bold text-gray-500">{{ a.codeArticle }}</span>
            <div class="min-w-0 flex-1">
              <b class="block truncate text-[13.5px] font-semibold">{{ a.designation }}</b>
              <span class="font-mono text-xs text-gray-400">Stock actuel : {{ a.stockActuel }}</span>
            </div>
          </div>

          <!-- Sens + quantité -->
          <div class="mb-3.5">
            <label class="etiquette">Sens de l'ajustement</label>
            <div class="grid grid-cols-2 gap-2.5">
              <button
                type="button"
                class="rounded-md border-[1.5px] py-2.5 text-sm font-semibold transition-all"
                [class]="sens() === 'plus' ? 'border-success bg-success-bg text-success' : 'border-line bg-surface text-gray-600 hover:border-ink-700'"
                (click)="sens.set('plus')"
              >
                + Ajouter
              </button>
              <button
                type="button"
                class="rounded-md border-[1.5px] py-2.5 text-sm font-semibold transition-all"
                [class]="sens() === 'moins' ? 'border-danger bg-danger-bg text-danger' : 'border-line bg-surface text-gray-600 hover:border-ink-700'"
                (click)="sens.set('moins')"
              >
                − Retirer
              </button>
      </div>
          </div>
          <div class="mb-3.5">
            <label class="etiquette" for="quantite">Quantité</label>
            <input id="quantite" type="number" min="1" [(ngModel)]="quantiteSaisie" class="champ font-mono" />
          </div>

          <!-- Motif -->
          <div class="mb-4">
            <label class="etiquette" for="motif">Motif *</label>
            <input
              id="motif"
              type="text"
              [(ngModel)]="motif"
              maxlength="200"
              placeholder="Ex. : erreur de saisie, casse, inventaire annuel…"
              class="champ"
            />
          </div>

          <!-- Aperçu du nouveau stock -->
          <div class="mb-4 flex items-center justify-between rounded-md bg-surface-alt px-4 py-3 text-sm">
            <span class="font-semibold text-gray-600">Stock après ajustement</span>
            <span class="font-mono text-base font-bold" [class]="stockApres() < 0 ? 'text-danger' : ''">{{ stockApres() }}</span>
          </div>

          @if (erreur()) {
            <div class="mb-4 rounded-sm border border-danger/30 bg-danger-bg px-3.5 py-2.5 text-sm text-danger">{{ erreur() }}</div>
          }

          <div class="flex gap-2.5 max-sm:grid max-sm:grid-cols-1">
            <button type="button" class="btn btn-secondaire flex-1" [disabled]="envoiEnCours()" (click)="fermer()">Annuler</button>
            <button type="button" class="btn btn-or flex-1" [disabled]="!motifValide || envoiEnCours() || quantite <= 0" (click)="confirmer()">
              {{ envoiEnCours() ? 'Enregistrement…' : "Enregistrer l'ajustement" }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class AjustementStockModal {
  private readonly rapportsService = inject(RapportsService);

  /** Contrôlé par l'écran parent : ouvrir(a) / fermer(). */
  readonly ouvert = signal(false);
  readonly article = signal<ArticleStock | null>(null);
  /** Émis avec le mouvement créé, pour resynchronisation par l'écran parent. */
  readonly confirme = output<MvtStk>();

  readonly sens = signal<'plus' | 'moins'>('plus');
  quantiteSaisie = 1;
  motif = '';
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  /** Quantité signée envoyée au backend. */
  get quantite(): number {
    return this.sens() === 'plus' ? Math.floor(this.quantiteSaisie) : -Math.floor(this.quantiteSaisie);
  }

  get motifValide(): boolean {
    return this.motif.trim().length > 0;
  }

  readonly stockApres = computed(() => {
    const a = this.article();
    if (!a) return 0;
    return a.stockActuel + this.quantite;
  });

  ouvrir(a: ArticleStock): void {
    this.article.set(a);
    this.sens.set('plus');
    this.quantiteSaisie = 1;
    this.motif = '';
    this.erreur.set(null);
    this.ouvert.set(true);
  }

  fermer(): void {
    if (this.envoiEnCours()) return;
    this.ouvert.set(false);
  }

  confirmer(): void {
    const a = this.article();
    if (!a || !this.motifValide || this.quantite === 0 || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const dto: AjustementRequest = { articleId: a.articleId, quantite: this.quantite, motif: this.motif.trim() };
    this.rapportsService.ajuster(dto).subscribe({
      next: (mvt) => {
        this.envoiEnCours.set(false);
        this.ouvert.set(false);
        this.confirme.emit(mvt);
      },
      error: (err) => {
        this.envoiEnCours.set(false);
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Ajustement refusé par le serveur.');
      },
    });
  }
}
