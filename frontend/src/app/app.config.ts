import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { errorInterceptor } from './core/interceptors/error-interceptor';
import { AuthService } from './core/services/services';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' })
    ),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    /**
     * Au lancement de l'app, on resynchronise le profil localStorage avec le
     * backend (GET /api/auth/me) AVANT la première navigation. Sinon les
     * guards routent sur un rôle potentiellement périmé : ex. le superadmin
     * promu ADMIN → SUPER_ADMIN par le DataInitializer resterait bloqué sur
     * l'espace entreprise. L'appel échoue en silence si non connecté.
     */
    provideAppInitializer(() => firstValueFrom(inject(AuthService).syncSession()).then(() => void 0)),
  ]
};
