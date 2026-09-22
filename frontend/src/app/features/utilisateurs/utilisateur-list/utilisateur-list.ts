import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UtilisateurService } from '../services/utilisateur-service';
import { AuthService } from '../../../core/services/services';
import { AppIcon } from '../../../shared/components/icon/icon';
import { ROLE_LABELS } from '../../../core/models/models';
import type { Utilisateur } from '../models/utilisateur.model';

/**
 * Liste des comptes utilisateurs de l'entreprise (espace ADMIN).
 *
 * Reprise du pattern ClientList : recherche instantanée, tableau desktop /
 * cartes mobile, suppression confirmée. Deux garde-fous spécifiques aux
 * comptes :
 * - un ADMIN ne peut pas supprimer ni rétrograder SON propre compte
 *   (sinon il peut se couper l'accès à la page sans s'en rendre compte) ;
 * - le dernier compte ADMIN actif est signalé dans l'UI pour éviter de
 *   se retrouver sans administrateur.
 */
@Component({
  selector: 'app-utilisateur-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './utilisateur-list.html',
})
export class UtilisateurList implements OnInit {
  private readonly utilisateurService = inject(UtilisateurService);
  private readonly auth = inject(AuthService);

  readonly recherche = signal('');
  readonly erreur = signal<string | null>(null);
  readonly supprimeEnCours = signal<number | null>(null);

  readonly roleLabels = ROLE_LABELS;
  readonly chargement = this.utilisateurService.chargement;
  readonly utilisateursFiltres = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.utilisateurService.utilisateurs();
    if (!q) return liste;
    return liste.filter(
      (u) =>
        u.nom.toLowerCase().includes(q) ||
        u.prenom.toLowerCase().includes(q) ||
        u.login.toLowerCase().includes(q) ||
        (u.mail ?? '').toLowerCase().includes(q)
    );
  });

  /** Id du compte connecté -- pour bloquer l'auto-suppression. */
  readonly moiId = this.auth.user()?.id ?? null;

  /** Nombre de comptes ADMIN (utile pour l'info-bulle du dernier admin). */
  readonly nbAdmins = computed(
    () => this.utilisateurService.utilisateurs().filter((u) => u.role === 'ADMIN').length
  );

  ngOnInit(): void {
    this.utilisateurService.loadAll().subscribe({
      error: () => this.erreur.set('Chargement impossible. Le serveur est-il démarré ?'),
    });
  }

  /** Vrai si le compte connecté est celui de la ligne (pas d'auto-suppression). */
  estMoi(u: Utilisateur): boolean {
    return this.moiId !== null && u.id === this.moiId;
  }

  /** Classes Tailwind du badge selon le rôle (accent or pour l'ADMIN). */
  badgeRole(role: Utilisateur['role']): string {
    switch (role) {
      case 'ADMIN':
        return 'bg-gold-100 text-gold-600';
      case 'GESTIONNAIRE':
        return 'bg-info-bg text-info';
      case 'VENDEUR':
        return 'bg-success-bg text-success';
      default:
        return 'bg-surface-alt text-gray-600';
    }
  }

  /** Message de confirmation adapté (auto-suppression / dernier admin). */
  messageConfirmation(u: Utilisateur): string {
    if (this.estMoi(u)) {
      return 'Vous ne pouvez pas supprimer votre propre compte.';
    }
    if (u.role === 'ADMIN' && this.nbAdmins() === 1) {
      return `« ${u.prenom} ${u.nom} » est le dernier administrateur. La suppression vous couperait l'accès à cette page. Confirmer ?`;
    }
    return `Supprimer le compte « ${u.prenom} ${u.nom} » ? Cette action est irréversible.`;
  }

  supprimer(u: Utilisateur): void {
    if (this.supprimeEnCours() !== null) return;
    if (!confirm(this.messageConfirmation(u))) return;
    this.supprimeEnCours.set(u.id);
    this.utilisateurService.delete(u.id).subscribe({
      next: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set(null);
        this.utilisateurService.loadAll().subscribe();
      },
      error: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set('Suppression impossible. Le compte est peut-être lié à des mouvements de stock.');
      },
    });
  }
}
