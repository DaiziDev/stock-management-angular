import { Component, computed, inject } from '@angular/core';
import { SidebarService } from '../layout.service';
import { AuthService } from '../../services/services';
import { ThemeService } from '../../services/theme.service';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Barre supérieure. La pill "entreprise" est alimentée par le nom réel
 * renvoyé par le backend (LoginResponse.user.entrepriseNom) — plus aucune
 * valeur codée en dur. Elle est masquée pour le SUPER_ADMIN : la console
 * plateforme n'est rattachée à aucune entreprise cliente.
 */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [AppIcon],
  template: `
    <nav
      class="fixed right-0 top-0 z-40 flex h-[66px] items-center gap-3.5 border-b border-line bg-canvas/85 pl-[calc(var(--sbw)+18px)] pr-7 backdrop-blur-[10px] transition-[left] duration-[350ms] ease-soft max-md:left-0 max-md:pl-4 max-md:pr-4"
    >
      <button
        type="button"
        class="hidden h-9 w-9 items-center justify-center rounded-[9px] text-gray-600 hover:bg-surface-alt max-md:flex"
        (click)="toggleSidebar()"
        aria-label="Menu"
      >
        <app-icon name="menu" />
      </button>

      <div class="relative max-w-[340px] flex-1 max-md:max-w-none">
        <app-icon class="pointer-events-none absolute left-[13px] top-1/2 -translate-y-1/2 text-gray-400" name="search" />
        <input
          type="text"
          placeholder="Rechercher..."
          class="w-full rounded-[22px] border-[1.5px] border-line bg-surface-alt py-2 pl-[37px] pr-3.5 text-sm transition-all duration-200 focus:border-ink-700 focus:bg-white focus:shadow-xs focus:outline-none"
        />
      </div>

      <div class="flex-1"></div>

      <div class="flex items-center gap-3">
        @if (estTenant()) {
          <button
            type="button"
            class="relative flex cursor-pointer items-center gap-2.5 rounded-[22px] border-[1.5px] border-line bg-surface py-[6px] pl-[9px] pr-[13px] text-sm font-semibold transition-colors duration-200 hover:border-ink-700"
          >
            <span class="h-[7px] w-[7px] rounded-full bg-success shadow-[0_0_0_2.5px_rgba(30,154,85,0.2)]"></span>
            {{ entrepriseNom() }}
            <app-icon name="chevron-down" [size]="13" class="text-gray-400" />
          </button>
        } @else {
          <span
            class="inline-flex items-center gap-2 rounded-[22px] border-[1.5px] border-gold-500/40 bg-gold-100 py-[6px] pl-[13px] pr-[13px] font-mono text-xs font-bold uppercase tracking-[0.08em] text-gold-600"
          >
            Console plateforme
          </span>
        }

        <button
          type="button"
          (click)="toggleTheme()"
          [attr.aria-label]="estSombre() ? 'Passer en mode clair' : 'Passer en mode sombre'"
          [title]="estSombre() ? 'Passer en mode clair' : 'Passer en mode sombre'"
          class="flex h-[37px] w-[37px] items-center justify-center rounded-full text-gray-600 transition-colors duration-[180ms] hover:bg-surface-alt"
        >
          <app-icon [name]="estSombre() ? 'sun' : 'moon'" />
        </button>

        <button
          type="button"
          aria-label="Notifications"
          class="relative flex h-[37px] w-[37px] items-center justify-center rounded-full text-gray-600 transition-colors duration-[180ms] hover:bg-surface-alt"
        >
          <app-icon name="bell" />
          <span class="absolute right-1.5 top-[5px] h-2 w-2 rounded-full border-2 border-surface bg-danger"></span>
        </button>
      </div>
    </nav>
  `,
})
export class Navbar {
  private readonly sidebar = inject(SidebarService);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);

  /** Le pill entreprise n'a de sens que pour un compte d'entreprise. */
  readonly estTenant = computed(() => this.auth.user()?.role !== 'SUPER_ADMIN');
  readonly entrepriseNom = computed(
    () => this.auth.user()?.entrepriseName ?? this.auth.user()?.nom ?? 'Entreprise'
  );

  /** Thème réellement affiché (résout le mode 'system' selon l'OS). */
  readonly estSombre = this.theme.isDark;

  toggleTheme(): void {
    this.theme.setMode(this.theme.isDark() ? 'light' : 'dark');
  }

  toggleSidebar(): void {
    this.sidebar.toggle();
  }
}
