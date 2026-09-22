import { Routes } from '@angular/router';
import { UtilisateurList } from './utilisateur-list/utilisateur-list';
import { UtilisateurForm } from './utilisateur-form/utilisateur-form';

export const UTILISATEUR_ROUTES: Routes = [
  { path: '', component: UtilisateurList },
  { path: 'nouveau', component: UtilisateurForm },
  { path: ':id/editer', component: UtilisateurForm },
];
