import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FournisseurService } from '../services/fournisseur-service';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Liste des fournisseurs : recherche instantanée (nom, ville, mail),
 * tableau desktop / cartes mobile, suppression confirmée.
 */
@Component({
  selector: 'app-fournisseur-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './fournisseur-list.html',
})
export class FournisseurList implements OnInit {
  private readonly fournisseurService = inject(FournisseurService);

  readonly recherche = signal('');
  readonly erreur = signal<string | null>(null);
  readonly supprimeEnCours = signal<number | null>(null);

  readonly chargement = this.fournisseurService.chargement;
  readonly fournisseursFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.fournisseurService.fournisseurs();
    if (!q) return liste;
    return liste.filter(
      (f) =>
        f.nom.toLowerCase().includes(q) ||
        (f.ville ?? '').toLowerCase().includes(q) ||
        (f.mail ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.fournisseurService.loadAll().subscribe({ error: () => this.erreur.set('Chargement impossible. Le serveur est-il démarré ?') });
  }

  supprimer(f: { id: number; nom: string }): void {
    if (this.supprimeEnCours() !== null) return;
    if (!confirm(`Supprimer le fournisseur « ${f.nom} » ? Cette action est irréversible.`)) return;
    this.supprimeEnCours.set(f.id);
    this.fournisseurService.delete(f.id).subscribe({
      next: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set(null);
        this.fournisseurService.loadAll().subscribe();
      },
      error: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set('Suppression impossible : le fournisseur est peut-être référencé par des commandes.');
      },
    });
  }
}
