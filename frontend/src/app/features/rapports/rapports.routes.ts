import { Routes } from '@angular/router';
import { Rapports } from './rapports/rapports';
import { MouvementsStock } from './mouvements-stock/mouvements-stock';

/**
 * Deux pages jumelles partageant le même service et le même modal
 * d'ajustement. Deux tableaux distincts : chaque route de premier niveau
 * (/rapports, /mouvements-stock) charge le sien et rend son écran sur ''.
 */
export const RAPPORTS_ROUTES: Routes = [{ path: '', component: Rapports }];

export const MOUVEMENTS_ROUTES: Routes = [{ path: '', component: MouvementsStock }];
