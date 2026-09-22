import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClientService } from '../services/client-service';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Liste des clients : recherche instantanée (nom, prénom, ville, mail),
 * tableau desktop / cartes mobile, suppression confirmée.
 */
@Component({
  selector: 'app-client-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './client-list.html',
})
export class ClientList implements OnInit {
  private readonly clientService = inject(ClientService);

  readonly recherche = signal('');
  readonly erreur = signal<string | null>(null);
  readonly supprimeEnCours = signal<number | null>(null);

  readonly chargement = this.clientService.chargement;
  readonly clientsFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.clientService.clients();
    if (!q) return liste;
    return liste.filter(
      (c) =>
        c.nom.toLowerCase().includes(q) ||
        c.prenom.toLowerCase().includes(q) ||
        (c.ville ?? '').toLowerCase().includes(q) ||
        (c.mail ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.clientService.loadAll().subscribe({ error: () => this.erreur.set('Chargement impossible. Le serveur est-il démarré ?') });
  }

  supprimer(c: { id: number; nom: string; prenom: string }): void {
    if (this.supprimeEnCours() !== null) return;
    if (!confirm(`Supprimer le client « ${c.prenom} ${c.nom} » ? Cette action est irréversible.`)) return;
    this.supprimeEnCours.set(c.id);
    this.clientService.delete(c.id).subscribe({
      next: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set(null);
        this.clientService.loadAll().subscribe();
      },
      error: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set('Suppression impossible : le client est peut-être référencé par des commandes ou ventes.');
      },
    });
  }
}
