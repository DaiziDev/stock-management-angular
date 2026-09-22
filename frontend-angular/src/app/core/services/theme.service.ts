import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

export type ThemeMode = 'light' | 'dark' | 'system';

const THEME_KEY = 'sgs.theme';

/**
 * Gestion du thème clair/sombre.
 *
 * Trois modes (mémorisés dans localStorage sous `sgs.theme`) :
 * - 'light'  : forcé clair, quel que soit l'OS ;
 * - 'dark'   : forcé sombre ;
 * - 'system' : suit `prefers-color-scheme` (mode par défaut).
 *
 * Le service applique/retire la classe `.dark` sur `<html>` : c'est elle qui
 * bascule les tokens du design system (styles.css, section 2).
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly media = this.document.defaultView?.matchMedia('(prefers-color-scheme: dark)') ?? null;

  /** Préférence de l'OS, sous forme de signal pour rester réactif. */
  private readonly osPrefersDark = signal(this.media?.matches ?? false);

  /** Mode choisi par l'utilisateur ('system' par défaut). */
  readonly mode = signal<ThemeMode>(this.restore());

  /** Le thème réellement affiché (résout 'system' selon l'OS). */
  readonly isDark = computed(
    () => this.mode() === 'dark' || (this.mode() === 'system' && this.osPrefersDark())
  );

  constructor() {
    // Bascule la classe .dark à chaque changement (mode OU préférence OS).
    effect(() => {
      this.document.documentElement.classList.toggle('dark', this.isDark());
    });

    // En mode 'system', suit l'OS en direct : l'utilisateur change le thème
    // de son OS → l'app bascule sans rechargement.
    this.media?.addEventListener('change', (e) => this.osPrefersDark.set(e.matches));
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
    localStorage.setItem(THEME_KEY, mode);
  }

  private restore(): ThemeMode {
    const raw = localStorage.getItem(THEME_KEY);
    return raw === 'light' || raw === 'dark' || raw === 'system' ? raw : 'system';
  }
}
