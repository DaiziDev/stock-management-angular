/**
 * Couleurs des graphiques Chart.js, résolues depuis les tokens CSS du
 * design system (styles.css §1) au moment de l'appel. Comme les tokens sont
 * réassignés par la classe `.dark` (styles.css §2), re-resoudre les couleurs
 * après un changement de thème suffit à recolorer les graphiques — d'où le
 * `chartTheme()` appelé depuis un computed qui dépend de ThemeService.isDark().
 *
 * Chaque lecture a un fallback sur la valeur claire du token : si la
 * variable n'existe pas (ex: test hors navigateur), les graphiques restent
 * lisibles au lieu de tout passer en noir invisible.
 */
export interface ChartTheme {
  /** Texte principal (labels d'axes category). */
  textPrimary: string;
  /** Texte secondaire (graduations, légende). */
  textSecondary: string;
  /** Grille et bordures de tooltip. */
  line: string;
  /** Fond des tooltips (surface, redéfinie en sombre). */
  surface: string;
  /** Entrées de stock / barres du top articles (accent marque). */
  success: string;
  /** Sorties de stock. */
  danger: string;
  /** Accent marque (barres du top). */
  gold: string;
}

export function chartTheme(): ChartTheme {
  const token = (name: string, fallback: string): string =>
    getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;

  return {
    textPrimary: token('--color-gray-700', '#33403f'),
    textSecondary: token('--color-gray-400', '#96a3a1'),
    line: token('--color-line', '#e6e8eb'),
    surface: token('--color-surface', '#ffffff'),
    success: token('--color-success', '#1e9a55'),
    danger: token('--color-danger', '#dd4b3e'),
    gold: token('--color-gold-500', '#c9922e'),
  };
}

/**
 * Options de tooltip partagées par tous les graphiques : mêmes angles,
 * mêmes couleurs que les cartes de l'UI (radius-md, bordure line).
 */
export function chartTooltip(theme: ChartTheme) {
  return {
    backgroundColor: theme.surface,
    titleColor: theme.textPrimary,
    bodyColor: theme.textSecondary,
    borderColor: theme.line,
    borderWidth: 1,
    padding: 10,
    cornerRadius: 9,
    boxPadding: 4,
  };
}
