import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Sidebar } from './sidebar';
import { AuthService } from '../../services/services';
import type { CurrentUser } from '../../models/models';

describe('Sidebar — filtre par rôle', () => {
  function createSidebar(role: 'ADMIN' | 'VENDEUR' | 'GESTIONNAIRE') {
    TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const auth = TestBed.inject(AuthService);
    // Anciennement auth.login() (qui appelait le backend) : ici on pose
    // directement le profil, ce qui suffit pour tester le filtre de menu.
    const user: CurrentUser = {
      id: 1,
      nom: 'Test ' + role,
      role,
      entrepriseId: 7,
      entrepriseName: 'SGS',
      login: `${role.toLowerCase()}@sgs.local`,
    };
    localStorage.setItem('sgs.currentUser', JSON.stringify(user));
    auth.user.set(user);

    const fixture = TestBed.createComponent(Sidebar);
    fixture.detectChanges();
    return { fixture, auth };
  }

  it('ADMIN voit les 12 items du menu (5 groupes)', () => {
    const { fixture } = createSidebar('ADMIN');
    const items = fixture.nativeElement.querySelectorAll('.sb-item');

    expect(items.length).toBe(12);
    // Groupe Organisation visible uniquement pour ADMIN.
    const labels = [...fixture.nativeElement.querySelectorAll('.sb-group-label')].map(
      (el: Element) => el.textContent
    );
    expect(labels).toContain('Organisation');
  });

  it('VENDEUR ne voit que 4 items et aucun groupe Organisation', () => {
    const { fixture } = createSidebar('VENDEUR');
    const items = fixture.nativeElement.querySelectorAll('.sb-item');
    const labels = [...items].map((el: Element) => el.textContent?.trim());

    expect(items.length).toBe(4);
    expect(labels).toContain('Tableau de bord');
    expect(labels).toContain('Articles');
    expect(labels).toContain('Clients');
    expect(labels).toContain('Point de vente');
    expect(labels).not.toContain('Utilisateurs & rôles');
    expect(labels).not.toContain('Commandes client');

    const groups = [...fixture.nativeElement.querySelectorAll('.sb-group-label')].map(
      (el: Element) => el.textContent
    );
    expect(groups).not.toContain('Organisation');
  });

  it('GESTIONNAIRE voit 10 items (tout sauf Organisation)', () => {
    const { fixture } = createSidebar('GESTIONNAIRE');
    const items = fixture.nativeElement.querySelectorAll('.sb-item');

    expect(items.length).toBe(10);
  });
});
