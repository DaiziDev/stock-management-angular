import { Component, input } from '@angular/core';
import { AppIcon } from '../../shared/components/icon/icon';

/**
 * Placeholder propre pour les routes du menu dont le module n'est pas
 * encore implémenté (rapports, commandes, mouvements de stock,
 * utilisateurs). Évite les pages blanches sous un lien de la sidebar.
 */
@Component({
  selector: 'app-bientot',
  standalone: true,
  imports: [AppIcon],
  template: `
    <div class="animate-view flex min-h-[50vh] items-center justify-center">
      <div class="carte max-w-md p-10 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-lg bg-gold-100 text-gold-600">
          <app-icon name="hammer" [size]="24" />
        </div>
        <h1 class="font-display text-2xl font-[650]">{{ titre() }}</h1>
        <p class="mt-2 text-sm leading-relaxed text-gray-600">
          Ce module est en cours de construction. Il apparaîtra ici dès sa mise en service.
        </p>
        <span class="mt-4 inline-block rounded-full bg-surface-alt px-3 py-1 font-mono text-2xs uppercase tracking-[0.08em] text-gray-400">
          Bientôt disponible
        </span>
      </div>
    </div>
  `,
})
export class BientotComponent {
  readonly titre = input('Module en construction');
}
