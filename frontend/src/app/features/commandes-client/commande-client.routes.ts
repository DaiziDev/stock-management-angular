import { Routes } from '@angular/router';
import { CommandeClientList } from './commande-client-list/commande-client-list';
import { CommandeClientForm } from './commande-client-form/commande-client-form';

export const COMMANDE_CLIENT_ROUTES: Routes = [
  { path: '', component: CommandeClientList },
  { path: 'nouvelle', component: CommandeClientForm },
];
