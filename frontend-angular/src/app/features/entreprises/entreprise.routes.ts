import { Routes } from '@angular/router';
import { EntrepriseList } from './entreprise-list/entreprise-list';
import { EntrepriseForm } from './entreprise-form/entreprise-form';

export const ENTREPRISE_ROUTES: Routes = [
  { path: '', component: EntrepriseList },
  { path: 'nouveau', component: EntrepriseForm },
  { path: ':id/edifier', component: EntrepriseForm },
];
