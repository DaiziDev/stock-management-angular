import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FournisseurService } from '../services/fournisseur-service';
import { FournisseurRequest } from '../models/fournisseur.model';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Création / édition d'un fournisseur. Même structure que le client,
 * sans le prénom (une entreprise, pas une personne physique).
 */
@Component({
  selector: 'app-fournisseur-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './fournisseur-form.html',
})
export class FournisseurForm implements OnInit {
  private fb = inject(FormBuilder);
  private fournisseurService = inject(FournisseurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  form = this.fb.group({
    nom: ['', Validators.required],
    adresse1: [''],
    adresse2: [''],
    ville: [''],
    codePostal: [''],
    pays: [''],
    mail: ['', Validators.email],
    numTel: [''],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    this.editId = Number(idParam);
    this.fournisseurService.loadAll().subscribe({
      next: (liste) => {
        const existant = liste.find((f) => f.id === this.editId);
        if (existant) this.form.patchValue(existant);
      },
      error: () => this.erreur.set('Impossible de charger le fournisseur.'),
    });
  }

  enregistrer(): void {
    if (this.form.invalid || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const raw = this.form.getRawValue();
    const dto = Object.fromEntries(
      Object.entries(raw).map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v])
    ) as unknown as FournisseurRequest;

    const action = this.editId
      ? this.fournisseurService.update(this.editId, dto)
      : this.fournisseurService.create(dto);

    action.subscribe({
      next: () => this.router.navigate(['/fournisseurs']),
      error: (err) => {
        this.envoiEnCours.set(false);
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Enregistrement impossible. Vérifiez les champs et réessayez.');
      },
    });
  }
}
