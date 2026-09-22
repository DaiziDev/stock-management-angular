import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ArticleService } from '../services/article-service';
import { CategorieService } from '../../categories/services/categorie-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type ArticleRequest } from '../models/article.model';

/**
 * Création / édition d'un article, avec photo.
 *
 * La photo est choisie depuis l'appareil, compressée côté navigateur
 * (canvas, ~440px, JPEG 72%) puis stockée en data URL base64 — le backend
 * la persiste telle quelle dans une colonne TEXT, sans stockage fichier
 * ni endpoint d'upload supplémentaire.
 */
@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './article-form.html',
})
export class ArticleForm implements OnInit {
  private fb = inject(FormBuilder);
  private articleService = inject(ArticleService);
  protected categorieService = inject(CategorieService); // protected : utilisé dans le template
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  /** Aperçu de la photo (data URL) — géré hors du FormGroup. */
  readonly photo = signal<string | null>(null);
  readonly photoEnCours = signal(false);
  readonly erreurPhoto = signal<string | null>(null);

  /** Le navigateur nous redonne ce fichier quand on clique "Changer". */
  private fichierEnAttente: File | null = null;

  form = this.fb.group({
    codeArticle: ['', Validators.required],
    designation: ['', Validators.required],
    prixUnitaireHt: [0, [Validators.required, Validators.min(0)]],
    tauxTva: [19.25, [Validators.required, Validators.min(0), Validators.max(100)]],
    categorieId: [null as number | null, Validators.required],
  });

  ngOnInit(): void {
    this.categorieService.loadAll().subscribe();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    this.editId = Number(idParam);
    this.articleService.loadAll().subscribe({
      next: (liste) => {
        const existant = liste.find((a) => a.id === this.editId);
        if (existant) {
          this.form.patchValue({
            codeArticle: existant.codeArticle,
            designation: existant.designation,
            prixUnitaireHt: existant.prixUnitaireHt,
            tauxTva: existant.tauxTva,
            categorieId: existant.categorie?.id ?? null,
          });
          this.photo.set(existant.photo ?? null);
        }
      },
      error: () => this.erreur.set("Impossible de charger l'article."),
    });
  }

  /** Ouvre le sélecteur de fichier de l'appareil. */
  choisirPhoto(): void {
    if (this.photoEnCours()) return;
    this.erreurPhoto.set(null);
    this.fichierEnAttente = null;
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = () => {
      const fichier = input.files?.[0];
      if (fichier) this.compresserEtStocker(fichier);
    };
    input.click();
  }

  /**
   * Compresse l'image choisie : ~440px de large max, JPEG qualité 72%,
   * fond blanc si l'image a de la transparence (PNG logos…). Un refus
   * propre si ce n'est pas une image. ~440px ≈ 25-60 Ko en base64,
   * confortable pour une colonne TEXT et pour le rendu des vignettes.
   */
  private compresserEtStocker(fichier: File): void {
    if (!fichier.type.startsWith('image/')) {
      this.erreurPhoto.set('Le fichier choisi n\'est pas une image.');
      return;
    }
    this.photoEnCours.set(true);
    this.erreurPhoto.set(null);

    const reader = new FileReader();
    reader.onerror = () => {
      this.photoEnCours.set(false);
      this.erreurPhoto.set('Impossible de lire le fichier.');
    };
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => {
        this.photoEnCours.set(false);
        this.erreurPhoto.set('Image illisible ou corrompue.');
      };
      img.onload = () => {
        try {
          const LARGEUR_MAX = 440;
          const ratio = Math.min(1, LARGEUR_MAX / img.naturalWidth);
          const canvas = document.createElement('canvas');
          canvas.width = Math.max(1, Math.round(img.naturalWidth * ratio));
          canvas.height = Math.max(1, Math.round(img.naturalHeight * ratio));
          const ctx = canvas.getContext('2d');
          if (!ctx) throw new Error('canvas indisponible');
          ctx.fillStyle = '#ffffff';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          this.photo.set(canvas.toDataURL('image/jpeg', 0.72));
          this.photoEnCours.set(false);
        } catch {
          this.photoEnCours.set(false);
          this.erreurPhoto.set('Compression impossible sur cet appareil.');
        }
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(fichier);
  }

  /** Retire la photo (celle qui vient d'être choisie ou celle en base). */
  retirerPhoto(): void {
    this.photo.set(null);
    this.erreurPhoto.set(null);
  }

  enregistrer(): void {
    if (this.form.invalid || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    const dto = {
      ...(this.form.getRawValue() as unknown as ArticleRequest),
      photo: this.photo(),
    };

    const action = this.editId
      ? this.articleService.update(this.editId, dto)
      : this.articleService.create(dto);

    action.subscribe({
      next: () => this.router.navigate(['/articles']),
      error: (err) => {
        this.envoiEnCours.set(false);
        const msg = (err?.error?.message as string) ?? null;
        this.erreur.set(msg ?? 'Enregistrement impossible : le code article existe peut-être déjà.');
      },
    });
  }
}
