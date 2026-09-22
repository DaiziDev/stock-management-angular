import { Routes } from '@angular/router';
import { CommandeFournisseurList } from './commande-fournisseur-list/commande-fournisseur-list';
import { CommandeFournisseurForm } from './commande-fournisseur-form/commande-fournisseur-form';

export const COMMANDE_FOURNISSEUR_ROUTES: Routes = [
  { path: '', component: CommandeFournisseurList },
  { path: 'nouvelle', component: CommandeFournisseurForm },
];
