import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CategorieService } from '../services/categorie-service';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Liste des catégories : recherche instantanée, tableau desktop / cartes
 * mobile, suppression confirmée. Mêmes patterns que la console plateforme :
 * un seul langage visuel dans toute l'app.
 */
@Component({
  selector: 'app-categorie-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './categorie-list.html',
})
export class CategorieList implements OnInit {
  private readonly categorieService = inject(CategorieService);

  readonly recherche = signal('');
  readonly erreur = signal<string | null>(null);
  readonly supprimeEnCours = signal<number | null>(null);

  readonly chargement = this.categorieService.chargement;
  readonly categoriesFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.categorieService.categories();
    if (!q) return liste;
    return liste.filter(
      (c) => c.designation.toLowerCase().includes(q) || c.code.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.categorieService.loadAll().subscribe({ error: () => this.erreur.set('Chargement impossible. Le serveur est-il démarré ?') });
  }

  supprimer(c: { id: number; designation: string }): void {
    if (this.supprimeEnCours() !== null) return;
    if (!confirm(`Supprimer la catégorie « ${c.designation} » ? Cette action est irréversible.`)) return;
    this.supprimeEnCours.set(c.id);
    this.categorieService.delete(c.id).subscribe({
      next: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set(null);
        this.categorieService.loadAll().subscribe();
      },
      error: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set('Suppression impossible : la catégorie est peut-être utilisée par des articles.');
      },
    });
  }
}
