import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ArticleService } from '../services/article-service';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Liste des articles : recherche (désignation/code/catégorie), tableau
 * desktop / cartes mobile, suppression confirmée.
 */
@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [DecimalPipe, RouterLink, AppIcon],
  templateUrl: './article-list.html',
})
export class ArticlieList implements OnInit {
  private readonly articleService = inject(ArticleService);

  readonly recherche = signal('');
  readonly erreur = signal<string | null>(null);
  readonly supprimeEnCours = signal<number | null>(null);

  readonly chargement = this.articleService.chargement;
  readonly articlesFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.articleService.articles();
    if (!q) return liste;
    return liste.filter(
      (a) =>
        a.designation.toLowerCase().includes(q) ||
        a.codeArticle.toLowerCase().includes(q) ||
        (a.categorie?.designation ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.articleService.loadAll().subscribe({ error: () => this.erreur.set('Chargement impossible. Le serveur est-il démarré ?') });
  }

  supprimer(a: { id: number; designation: string }): void {
    if (this.supprimeEnCours() !== null) return;
    if (!confirm(`Supprimer l'article « ${a.designation} » ? Cette action est irréversible.`)) return;
    this.supprimeEnCours.set(a.id);
    this.articleService.delete(a.id).subscribe({
      next: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set(null);
        this.articleService.loadAll().subscribe();
      },
      error: () => {
        this.supprimeEnCours.set(null);
        this.erreur.set("Suppression impossible : l'article est peut-être référencé par des ventes ou commandes.");
      },
    });
  }
}
