import { Routes } from '@angular/router';
import { FournisseurList } from './fournisseur-list/fournisseur-list';
import { FournisseurForm } from './fournisseur-form/fournisseur-form';

export const FOURNISSEUR_ROUTES: Routes = [
  { path: '', component: FournisseurList },
  { path: 'nouveau', component: FournisseurForm },
  { path: ':id/editer', component: FournisseurForm },
];
