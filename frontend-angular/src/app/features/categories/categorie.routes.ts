import { Routes } from '@angular/router';
import { CategorieList } from './categorie-list/categorie-list';
import { CategorieForm } from './categorie-form/categorie-form';

export const CATEGORIE_ROUTES: Routes = [
  { path: '', component: CategorieList },
  { path: 'nouveau', component: CategorieForm },
  { path: ':id/editer', component: CategorieForm },
];