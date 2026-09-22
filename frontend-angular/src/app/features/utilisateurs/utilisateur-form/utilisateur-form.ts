import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UtilisateurService } from '../services/utilisateur-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { ALL_ROLES, ROLE_LABELS } from '../../../core/models/models';

/**
 * Création / édition d'un compte utilisateur de l'entreprise (espace ADMIN).
 *
 * Les deux modes n'ont PAS le même payload backend, d'où un seul composant
 * avec deux branches :
 * - CRÉATION → POST /api/auth/register : login + mot de passe requis,
 *   entrepriseId imposé par le code (celle de l'ADMIN connecté, jamais
 *   saisie -- le backend refuse de toute façon un autre tenant) ;
 * - ÉDITION → PUT /api/utilisateurs/{id} : ni login ni mot de passe (le
 *   backend ne les accepte pas dans UtilisateurUpdateDTO -- renommer un
 *   identifiant ou changer un mot de passe sont des opérations dédiées).
 */
@Component({
  selector: 'app-utilisateur-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './utilisateur-form.html',
})
export class UtilisateurForm implements OnInit {
  private fb = inject(FormBuilder);
  private utilisateurService = inject(UtilisateurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  /** Rôles assignables (SUPER_ADMIN exclu : ce n'est pas un rôle d'entreprise). */
  readonly roles = ALL_ROLES;
  readonly roleLabels = ROLE_LABELS;

  form = this.fb.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    login: ['', [Validators.required, Validators.email]],
    motDePasse: [''],
    mail: [''],
    numTel: [''],
    role: ['VENDEUR', Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      // Création : le mot de passe est requis dès l'affichage, pour que
      // l'état [disabled] du bouton reflète la validité réelle du formulaire.
      this.form.controls.motDePasse.setValidators([Validators.required, Validators.minLength(8)]);
      this.form.controls.motDePasse.updateValueAndValidity();
      return;
    }
    this.editId = Number(idParam);

    // En édition, on recharge la liste (déjà en cache dans le service la
    // plupart du temps) pour pré-remplir le formulaire.
    this.utilisateurService.loadAll().subscribe({
      next: (liste) => {
        const existant = liste.find((u) => u.id === this.editId);
        if (!existant) {
          this.erreur.set('Utilisateur introuvable.');
          return;
        }
        this.form.patchValue({
          prenom: existant.prenom,
          nom: existant.nom,
          login: existant.login,
          mail: existant.mail ?? '',
          numTel: existant.numTel ?? '',
          role: existant.role,
        });
        // Le login n'est PAS modifiable en édition (contrainte backend).
        this.form.controls.login.disable();
      },
      error: () => this.erreur.set('Impossible de charger l’utilisateur.'),
    });
  }

  enregistrer(): void {
    if (this.envoiEnCours()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const raw = this.form.getRawValue();
    // getRawValue() renvoie un type avec champs nullables dès qu'un contrôle
    // est désactivé (login en édition) -- d'où les ?? ''. Les champs vides
    // deviennent null (l'API attend des String nullables, pas "").
    const login = (raw.login ?? '').trim();
    const nom = (raw.nom ?? '').trim();
    const prenom = (raw.prenom ?? '').trim();
    const nul = (v: string) => (v.trim() === '' ? null : v.trim());

    const action$ =
      this.editId !== null
        ? this.utilisateurService.update(this.editId, {
            nom,
            prenom,
            mail: nul(raw.mail ?? ''),
            numTel: nul(raw.numTel ?? ''),
            role: raw.role as Parameters<UtilisateurService['update']>[1]['role'],
          })
        : this.utilisateurService.create({
            nom,
            prenom,
            login,
            motDePasse: raw.motDePasse ?? '',
            mail: nul(raw.mail ?? ''),
            numTel: nul(raw.numTel ?? ''),
            role: raw.role as Parameters<UtilisateurService['create']>[0]['role'],
            // L'entreprise est celle de l'ADMIN connecté : le backend
            // refuse explicitement toute autre valeur (RG-10).
            entrepriseId: this.utilisateurService.entrepriseCouranteId() ?? 0,
          });

    action$.subscribe({
      next: () => this.router.navigate(['/utilisateurs']),
      error: (err) => {
        this.envoiEnCours.set(false);
        // 403 = tentative hors de son périmètre ; 400 = login déjà pris…
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Enregistrement impossible. Vérifiez les champs et réessayez.');
      },
    });
  }
}
