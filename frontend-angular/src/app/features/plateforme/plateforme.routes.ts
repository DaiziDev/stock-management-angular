import { Routes } from '@angular/router';
import { PlateformeDashboard } from './plateforme-dashboard/plateforme-dashboard';
import { PlateformeEntreprises } from './plateforme-entreprises/plateforme-entreprises';

export const PLATEFORME_ROUTES: Routes = [
  { path: '', component: PlateformeDashboard },
  { path: 'entreprises', component: PlateformeEntreprises },
  {
    path: 'entreprises/:id/editer',
    loadComponent: () => import('./plateforme-entreprise-form/plateforme-entreprise-form').then((m) => m.PlateformeEntrepriseForm),
  },
];
