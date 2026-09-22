import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NAV, ROLE_LABELS } from '../../models/models';
import { AuthService } from '../../services/services';
import { SidebarService } from '../layout.service';
import { AppIcon } from '../../../shared/components/icon/icon';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, AppIcon],
  template: `
    <div
      class="sb-backdrop fixed inset-0 z-[45] bg-ink-950/45 transition-all duration-300 ease-soft {{ mobileOpen() ? 'opacity-100 visible' : 'opacity-0 invisible' }}"
      (click)="closeSidebar()"
    ></div>
    <aside
      class="sidebar fixed inset-y-0 left-0 z-50 flex w-[var(--sbw)] flex-col bg-ink-950 text-fog-100 transition-transform duration-[350ms] ease-soft {{ mobileOpen() ? 'max-md:translate-x-0' : 'max-md:-translate-x-full' }}"
    >
      <div class="sb-brand flex items-center gap-[11px] px-[22px] pb-4 pt-[22px]">
        <div class="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-[11px] bg-[linear-gradient(150deg,var(--color-gold-400),var(--color-gold-600))] font-display text-lg font-bold text-ink-950 shadow-[0_4px_14px_rgba(201,146,46,0.4)]">
          S
        </div>
        <span class="font-display text-lg font-[650] text-white">SGS</span>
      </div>

      <nav class="sb-nav no-scrollbar flex-1 overflow-y-auto px-3 py-2">
        @for (group of visibleGroups(); track group.group) {
          <div class="sb-group-label px-3 pb-[7px] pt-[17px] font-mono text-2xs uppercase tracking-[0.13em] text-fog-700">{{ group.group }}</div>
          @for (item of group.items; track item.key) {
            <a
              class="sb-item relative mb-0.5 flex items-center gap-[11px] rounded-[9px] px-3 py-[9.5px] text-sm font-medium text-fog-100/80 transition-colors duration-200 ease-soft hover:bg-white/5 hover:text-white [&>app-icon]:text-fog-500 hover:[&>app-icon]:text-fog-100 [&.active]:bg-gold-500/15 [&.active]:text-white [&.active>app-icon]:text-gold-400 [&.active]:before:absolute [&.active]:before:-left-3 [&.active]:before:top-[9px] [&.active]:before:bottom-[9px] [&.active]:before:w-[3px] [&.active]:before:rounded-full [&.active]:before:bg-gold-500"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: true }"
              (click)="closeSidebar()"
            >
              <app-icon [name]="item.icon" />
              <span>{{ item.label }}</span>
            </a>
          }
        }
      </nav>

      <div class="sb-footer border-t border-white/7 px-[22px] pb-5 pt-3.5">
        @if (user(); as u) {
          <div class="sb-user flex items-center gap-2.5">
            <div class="sb-avatar flex h-[33px] w-[33px] shrink-0 items-center justify-center rounded-full bg-[linear-gradient(150deg,var(--color-gold-400),var(--color-gold-600))] text-sm font-bold text-ink-950">
              {{ u.nom.charAt(0) }}
            </div>
            <div class="sb-user-info min-w-0 flex-1">
              <b class="block text-sm text-white">{{ u.nom }}</b>
              <span class="text-xs text-fog-600">{{ roleLabel(u.role) }}</span>
            </div>
            <button class="sb-logout ml-auto flex rounded-lg p-[7px] text-fog-600 hover:bg-white/8 hover:text-white" (click)="logout()" title="Déconnexion">
              <app-icon name="log-out" />
            </button>
          </div>
        }
      </div>
    </aside>
  `,
})
export class Sidebar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly sidebar = inject(SidebarService);

  readonly mobileOpen = this.sidebar.mobileOpen;
  readonly user = this.auth.user;

  readonly groups = NAV;

  readonly visibleGroups = computed(() =>
    this.groups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => this.auth.hasRole(...item.roles)),
      }))
      .filter((group) => group.items.length > 0)
  );

  roleLabel(role: string): string {
    return ROLE_LABELS[role as keyof typeof ROLE_LABELS] ?? role;
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  closeSidebar(): void {
    this.sidebar.close();
  }
}
