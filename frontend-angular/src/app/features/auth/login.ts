import { Component, inject, type OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/services';
import { type CurrentUser } from '../../core/models/models';
import { AppIcon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-login',
  imports: [FormsModule, AppIcon],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  identifiant = 'admin@sgs.local';
  password = '';
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    // Déjà connecté → on va directement sur SON espace (plateforme ou entreprise).
    if (this.auth.user()) {
      this.router.navigate([this.cible(this.auth.user(), null)]);
    }
  }

  submit(): void {
    if (this.loading) return;
    this.error = null;
    this.loading = true;

    this.auth.login(this.identifiant, this.password).subscribe({
      next: () => {
        // Retour à la page initialement demandée (paramètre redirect posé par
        // authGuard), à condition qu'elle corresponde à l'espace du rôle --
        // sinon (ex: token resté en localStorage après un changement de rôle)
        // on retombe sur l'accueil de SON espace.
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        this.router.navigate([this.cible(this.auth.user(), redirect)]);
      },
      error: () => {
        this.loading = false;
        this.error = 'Identifiant ou mot de passe incorrect.';
      },
    });
  }

  /**
   * Accueil après connexion : /plateforme pour l'opérateur (SUPER_ADMIN),
   * /dashboard pour les comptes d'entreprise. Le paramètre `redirect` n'est
   * honoré que s'il pointe dans le bon espace.
   */
  private cible(user: CurrentUser | null, redirect: string | null): string {
    const estPlateforme = user?.role === 'SUPER_ADMIN';
    if (redirect && redirect.startsWith('/')) {
      const redirectValide = estPlateforme
        ? redirect.startsWith('/plateforme')
        : !redirect.startsWith('/plateforme');
      if (redirectValide) return redirect;
    }
    return estPlateforme ? '/plateforme' : '/dashboard';
  }
}
