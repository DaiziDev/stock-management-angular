import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './services';
import type { LoginResponse } from '../models/models';

/** Réponse /api/auth/login conforme au backend (AuthTokensDTO). */
function reponseLogin(role: LoginResponse['user']['role']): LoginResponse {
  return {
    token: 'jwt-' + role,
    refreshToken: 'refresh-' + role,
    user: {
      id: 1,
      nom: 'Kamga',
      prenom: 'Ravel',
      login: 'admin@sgs.local',
      role,
      entrepriseId: 7,
      entrepriseNom: 'SGS',
    },
  };
}

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Simule POST /api/auth/login : capture le corps + répond. */
  function simulerLogin(role: LoginResponse['user']['role']) {
    let reponse: LoginResponse | undefined;
    service.login('admin@sgs.local', 'x').subscribe((r) => (reponse = r));
    const req = http.expectOne({ method: 'POST', url: 'http://localhost:8081/api/auth/login' });
    req.flush(reponseLogin(role));
    return reponse;
  }

  it('starts without a logged-in user', () => {
    expect(service.user()).toBeNull();
    expect(service.hasRole('ADMIN')).toBe(false);
  });

  it('logs in and stores the user + the two tokens', () => {
    simulerLogin('ADMIN');

    expect(service.user()?.role).toBe('ADMIN');
    expect(service.user()?.nom).toBe('Ravel Kamga');
    expect(service.user()?.login).toBe('admin@sgs.local');
    expect(service.hasRole('ADMIN')).toBe(true);
    expect(service.hasRole('VENDEUR')).toBe(false);
    expect(service.getToken()).toBe('jwt-ADMIN');
    expect(service.getRefreshToken()).toBe('refresh-ADMIN');
    expect(localStorage.getItem('sgs.refreshToken')).toBe('refresh-ADMIN');
  });

  it('checks any-of roles', () => {
    simulerLogin('VENDEUR');
    expect(service.hasRole('ADMIN', 'VENDEUR')).toBe(true);
    expect(service.hasRole('ADMIN', 'GESTIONNAIRE')).toBe(false);
  });

  it('persists the session in localStorage and restores it', () => {
    simulerLogin('GESTIONNAIRE');

    // Un nouveau service (rechargement de page) retrouve l'utilisateur.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const reloaded = TestBed.inject(AuthService);

    expect(reloaded.user()?.role).toBe('GESTIONNAIRE');
  });

  it('logs out and clears the session', () => {
    simulerLogin('ADMIN');
    service.logout();

    // Le refresh token est révoqué côté serveur (best-effort)…
    http.expectOne({ method: 'POST', url: 'http://localhost:8081/api/auth/logout' }).flush(null);
    // …et l'état local est purgé (tokens + profil).
    expect(service.user()).toBeNull();
    expect(localStorage.getItem('sgs.currentUser')).toBeNull();
    expect(localStorage.getItem('sgs.token')).toBeNull();
    expect(localStorage.getItem('sgs.refreshToken')).toBeNull();
  });

  it('refreshSession échange le refresh token contre un nouveau couple', () => {    simulerLogin('ADMIN');

    let session: LoginResponse | null = null;
    service.refreshSession().subscribe((r) => (session = r));

    const req = http.expectOne({ method: 'POST', url: 'http://localhost:8081/api/auth/refresh' });
    expect(req.request.body).toEqual({ refreshToken: 'refresh-ADMIN' });
    req.flush(reponseLogin('ADMIN'));

    expect(session).not.toBeNull();
    // Le couple de tokens a tourné.
    expect(service.getToken()).toBe('jwt-ADMIN');
    expect(service.getRefreshToken()).toBe('refresh-ADMIN');
  });

  it('refreshSession retourne null sans refresh token stocké', () => {    let session: LoginResponse | null | undefined;
    service.refreshSession().subscribe((r) => (session = r));

    expect(session).toBeNull();
    http.expectNone({ method: 'POST', url: 'http://localhost:8081/api/auth/refresh' });
  });
});
