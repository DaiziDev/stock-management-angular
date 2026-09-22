import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CategorieService } from '../services/categorie-service';
import { CategorieRequest } from '../models/categorie.model';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Création / édition d'une catégorie. En édition, la donnée est rechargée
 * depuis l'API : plus de dépendance à l'état de la page précédente.
 */
@Component({
  selector: 'app-categorie-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './categorie-form.html',
})
export class CategorieForm implements OnInit {
  private fb = inject(FormBuilder);
  private categorieService = inject(CategorieService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  form = this.fb.group({
    code: ['', Validators.required],
    designation: ['', Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    this.editId = Number(idParam);
    // Rechargement systématique : la liste n'est pas forcément en mémoire
    // (accès direct à l'URL) et un signal d'autre page peut être périmé.
    this.categorieService.loadAll().subscribe({
      next: (liste) => {
        const existant = liste.find((c) => c.id === this.editId);
        if (existant) this.form.patchValue(existant);
      },
      error: () => this.erreur.set("Impossible de charger la catégorie."),
    });
  }

  enregistrer(): void {
    if (this.form.invalid || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const dto = this.form.getRawValue() as CategorieRequest;
    const action = this.editId
      ? this.categorieService.update(this.editId, dto)
      : this.categorieService.create(dto);

    action.subscribe({
      next: () => this.router.navigate(['/categories']),
      error: (err) => {
        this.envoiEnCours.set(false);
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Enregistrement impossible : le code existe peut-être déjà.');
      },
    });
  }
}
