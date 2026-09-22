import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  Chart,
  LineController,
  BarController,
  LineElement,
  BarElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler,
  Legend,
  Tooltip,
  type ChartConfiguration,
} from 'chart.js';
import { ThemeService } from '../../../core/services/theme.service';

// Enregistrement tree-shakable : uniquement ce que les graphiques du
// dashboard utilisent (ligne + barres). Pas d'import 'chart.js/auto' qui
// embarquerait les ~20 contrôleurs et échelles pour rien.
Chart.register(
  LineController,
  BarController,
  LineElement,
  BarElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler,
  Legend,
  Tooltip
);

/**
 * Composant graphique réutilisable (Chart.js v4) intégré au design system :
 * - rendu sur <canvas> via une config Chart.js complète passée en input ;
 * - SE RECOLORE automatiquement au basculement clair/sombre : l'effect
 *   dépend du signal isDark de ThemeService ET de la config — quand l'un
 *   des deux change, le chart est détruit et reconstruit. Les couleurs de
 *   la config sont résolues depuis les tokens CSS (chart-theme.ts), qui
 *   sont réassignés par la classe .dark (styles.css §2) ;
 * - responsive : Chart.js gère le redimensionnement (la hauteur vient de
 *   l'input height, appliquée au conteneur).
 *
 * Usage : <app-chart [config]="maConfig()" [height]="230" />
 */
@Component({
  selector: 'app-chart',
  standalone: true,
  template: `<div class="relative w-full" [style.height.px]="height()">
    <canvas #canvas></canvas>
  </div>`,
})
export class AppChart implements AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly themeService = inject(ThemeService);

  /** Configuration Chart.js complète (labels, datasets, options). */
  readonly config = input.required<ChartConfiguration>();

  /** Hauteur du graphique en pixels (le canvas remplit le conteneur). */
  readonly height = input<number>(240);

  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  private chart?: Chart;

  constructor() {
    // Dépend du thème ET de la config : un changement de l'un ou de l'autre
    // reconstruit le chart. Le premier passage (chart pas encore créé) ne
    // fait rien — ngAfterViewInit s'en charge.
    effect(() => {
      this.themeService.isDark();
      this.config();
      if (this.chart) {
        this.rebuild();
      }
    });

    this.destroyRef.onDestroy(() => this.chart?.destroy());
  }

  ngAfterViewInit(): void {
    this.rebuild();
  }

  /**
   * (Re)crée le chart. On passe une copie superficielle de la config :
   * Chart.js mute l'objet reçu au constructeur, et réutiliser la même
   * référence mutée après destroy() ferait échouer la recoloration.
   */
  private rebuild(): void {
    const config = this.config();
    this.chart?.destroy();
    this.chart = new Chart(this.canvasRef().nativeElement, {
      ...config,
      options: { ...config.options },
    });
  }
}
