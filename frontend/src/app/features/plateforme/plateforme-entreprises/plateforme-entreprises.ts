import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PlateformeService } from '../services/plateforme-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type EntrepriseCliente } from '../models/plateforme.models';

/**
 * Gestion des entreprises clientes (console plateforme) : liste avec
 * recherche + formulaire d'onboarding "entreprise + premier ADMIN".
 *
 * Le formulaire d'onboarding est intégré à la page (replié par défaut) :
 * c'est UN geste métier côté backend (une transaction), et ça évite une
 * page intermédiaire. Le formulaire d'édition simple reste une route à part.
 */
@Component({
  selector: 'app-plateforme-entreprises',
  imports: [RouterLink, FormsModule, AppIcon],
  templateUrl: './plateforme-entreprises.html',
})
export class PlateformeEntreprises implements OnInit {
  private readonly plateformeService = inject(PlateformeService);

  readonly recherche = signal('');
  readonly formulaireOuvert = signal(false);
  readonly envoiEnCours = signal(false);
  readonly erreur = signal<string | null>(null);

  // Champs du formulaire d'onboarding (ngModel, comme la page login).
  nomEntreprise = '';
  adresse1 = '';
  adresse2 = '';
  ville = '';
  codePostal = '';
  pays = '';
  mailEntreprise = '';
  numTelEntreprise = '';
  adminPrenom = '';
  adminNom = '';
  adminLogin = '';
  adminMotDePasse = '';
  adminMail = '';
  adminNumTel = '';

  ngOnInit(): void {
    this.plateformeService.loadAll();
  }

  readonly entreprisesFiltrees = () => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.plateformeService.entreprises();
    if (!q) return liste;
    return liste.filter(
      (e) =>
        e.nom.toLowerCase().includes(q) ||
        (e.ville ?? '').toLowerCase().includes(q) ||
        (e.mail ?? '').toLowerCase().includes(q)
    );
  };

  toggleFormulaire(): void {
    this.formulaireOuvert.update((v) => !v);
    this.erreur.set(null);
  }

  onboarder(): void {
    if (this.envoiEnCours()) return;
    if (!this.nomEntreprise.trim() || !this.adminLogin.trim() || !this.adminMotDePasse.trim()) {
      this.erreur.set('Le nom de l\u2019entreprise, le login et le mot de passe de l\u2019admin sont obligatoires.');
      return;
    }
    this.envoiEnCours.set(true);
    this.erreur.set(null);

    this.plateformeService
      .onboarder({
        nomEntreprise: this.nomEntreprise.trim(),
        adresse1: this.adresse1.trim() || null,
        adresse2: this.adresse2.trim() || null,
        ville: this.ville.trim() || null,
        codePostal: this.codePostal.trim() || null,
        pays: this.pays.trim() || null,
        mailEntreprise: this.mailEntreprise.trim() || null,
        numTelEntreprise: this.numTelEntreprise.trim() || null,
        adminPrenom: this.adminPrenom.trim(),
        adminNom: this.adminNom.trim(),
        adminLogin: this.adminLogin.trim(),
        adminMotDePasse: this.adminMotDePasse,
        adminMail: this.adminMail.trim() || null,
        adminNumTel: this.adminNumTel.trim() || null,
      })
      .subscribe({
        next: () => {
          this.envoiEnCours.set(false);
          this.formulaireOuvert.set(false);
          this.reinitialiserFormulaire();
          this.plateformeService.rafraichir();
        },
        error: (err) => {
          this.envoiEnCours.set(false);
          // Le backend renvoie un ApiError { message } : nom d'entreprise ou
          // login déjà pris, etc. On l'affiche tel quel plutôt qu'un message
          // générique -- sinon l'échec paraît "mystérieux".
          this.erreur.set(
            this.plateformeService.erreurLisible(
              err,
              'Création impossible : vérifiez que le nom de l\u2019entreprise et le login de l\u2019admin sont uniques.'
            )
          );
        },
      });
  }

  private reinitialiserFormulaire(): void {
    this.nomEntreprise = '';
    this.adresse1 = '';
    this.adresse2 = '';
    this.ville = '';
    this.codePostal = '';
    this.pays = '';
    this.mailEntreprise = '';
    this.numTelEntreprise = '';
    this.adminPrenom = '';
    this.adminNom = '';
    this.adminLogin = '';
    this.adminMotDePasse = '';
    this.adminMail = '';
    this.adminNumTel = '';
  }
}
