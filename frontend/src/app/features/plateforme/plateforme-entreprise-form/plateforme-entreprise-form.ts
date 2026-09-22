import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PlateformeService, type EntrepriseUpdatePayload } from '../services/plateforme-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type EntrepriseCliente } from '../models/plateforme.models';

/**
 * Édition des coordonnées d'une entreprise cliente (SUPER_ADMIN).
 * Le nom est non modifiable ici : c'est l'identifiant de cloisonnement
 * (RG-10) affiché partout chez le client -- un renommage mériterait un
 * parcours dédié avec confirmation, pas un champ de formulaire.
 */
@Component({
  selector: 'app-plateforme-entreprise-form',
  imports: [FormsModule, RouterLink, AppIcon],
  templateUrl: './plateforme-entreprise-form.html',
})
export class PlateformeEntrepriseForm implements OnInit {
  private readonly plateformeService = inject(PlateformeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly entreprise = signal<EntrepriseCliente | null>(null);
  readonly chargement = signal(true);
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  /** Localisation courte pour la carte d'identité (le template n'a pas accès à Boolean). */
  readonly localisation = computed(() => {
    const e = this.entreprise();
    if (!e) return '';
    return [e.ville, e.pays].filter((v) => !!v).join(', ');
  });

  // Champs éditables (ngModel).
  adresse1 = '';
  adresse2 = '';
  ville = '';
  codePostal = '';
  pays = '';
  mail = '';
  numTel = '';

  ngOnInit(): void {
    // Chargement direct depuis l'API par l'id de la route : fiable, sans
    // dépendre de la liste chargée par la page précédente (la version
    // setTimeout(300) était une course perdue d'avance en accès direct).
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.chargement.set(false);
      return;
    }
    this.plateformeService.findById(id).subscribe({
      next: (e) => {
        this.entreprise.set(e);
        this.adresse1 = e.adresse1 ?? '';
        this.adresse2 = e.adresse2 ?? '';
        this.ville = e.ville ?? '';
        this.codePostal = e.codePostal ?? '';
        this.pays = e.pays ?? '';
        this.mail = e.mail ?? '';
        this.numTel = e.numTel ?? '';
        this.chargement.set(false);
      },
      error: () => {
        this.entreprise.set(null);
        this.chargement.set(false);
      },
    });
  }

  enregistrer(): void {
    const e = this.entreprise();
    if (!e || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const payload: EntrepriseUpdatePayload = {
      adresse1: this.adresse1.trim() || null,
      adresse2: this.adresse2.trim() || null,
      ville: this.ville.trim() || null,
      codePostal: this.codePostal.trim() || null,
      pays: this.pays.trim() || null,
      mail: this.mail.trim() || null,
      numTel: this.numTel.trim() || null,
    };

    this.plateformeService.update(e.id, payload).subscribe({
      next: () => {
        this.envoiEnCours.set(false);
        this.plateformeService.rafraichir();
        this.router.navigate(['/plateforme/entreprises']);
      },
      error: (err) => {
        this.envoiEnCours.set(false);
        this.erreur.set(this.plateformeService.erreurLisible(err, 'Enregistrement impossible. Réessayez.'));
      },
    });
  }
}
