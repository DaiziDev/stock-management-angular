import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SidebarService {
  readonly mobileOpen = signal(false);

  toggle(): void {
    this.mobileOpen.update((v) => !v);
  }

  close(): void {
    this.mobileOpen.set(false);
  }
}
