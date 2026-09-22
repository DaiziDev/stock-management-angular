import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClientService } from '../services/client-service';
import { ClientRequest } from '../models/client.model';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * Création / édition d'un client. Les champs d'adresse à plat reprennent le
 * choix du backend (ClientRequestDTO, même forme que l'Adresse embarquée).
 */
@Component({
  selector: 'app-client-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './client-form.html',
})
export class ClientForm implements OnInit {
  private fb = inject(FormBuilder);
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  form = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
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
    this.clientService.loadAll().subscribe({
      next: (liste) => {
        const existant = liste.find((c) => c.id === this.editId);
        if (existant) this.form.patchValue(existant);
      },
      error: () => this.erreur.set('Impossible de charger le client.'),
    });
  }

  enregistrer(): void {
    if (this.form.invalid || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const raw = this.form.getRawValue();
    // Champs vides -> null (l'API attend des String nullables, pas "").
    const dto = Object.fromEntries(
      Object.entries(raw).map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v])
    ) as unknown as ClientRequest;

    const action = this.editId
      ? this.clientService.update(this.editId, dto)
      : this.clientService.create(dto);

    action.subscribe({
      next: () => this.router.navigate(['/clients']),
      error: (err) => {
        this.envoiEnCours.set(false);
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Enregistrement impossible. Vérifiez les champs et réessayez.');
      },
    });
  }
}
