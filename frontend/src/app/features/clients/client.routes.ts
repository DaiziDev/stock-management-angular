import { Routes } from '@angular/router';
import { ClientList } from './client-list/client-list';
import { ClientForm } from './client-form/client-form';

export const CLIENT_ROUTES: Routes = [
  { path: '', component: ClientList },
  { path: 'nouveau', component: ClientForm },
  { path: ':id/editer', component: ClientForm },
];
