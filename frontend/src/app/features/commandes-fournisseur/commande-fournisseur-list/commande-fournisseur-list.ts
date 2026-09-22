import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { type LigneCommandeFournisseur } from '../models/commande-fournisseur.model';
import { RouterLink } from '@angular/router';
import { CommandeFournisseurService } from '../services/commande-fournisseur-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type CommandeFournisseur, type StatutCommandeFournisseur } from '../models/commande-fournisseur.model';

/**
 * Liste des commandes fournisseur : filtres statut + recherche, tableau
 * desktop / cartes mobile, panneau de détail et actions de cycle de vie —
 * Réceptionner génère les ENTRÉES de stock côté backend (une commande déjà
 * reçue est refusée, le stock serait compté deux fois), Annuler ne touche
 * pas au stock. Miroir du module commandes client, prix en HT.
 */
@Component({
  selector: 'app-commande-fournisseur-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './commande-fournisseur-list.html',
})
export class CommandeFournisseurList implements OnInit {
  private readonly commandeService = inject(CommandeFournisseurService);

  readonly commandes = signal<CommandeFournisseur[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly recherche = signal('');
  readonly filtreStatut = signal<'TOUS' | StatutCommandeFournisseur>('TOUS');

  /** Choix de filtres typés (le template ne peut pas typer un littéral inline). */
  readonly filtres: { v: 'TOUS' | StatutCommandeFournisseur; l: string }[] = [
    { v: 'TOUS', l: 'Toutes' },
    { v: 'EN_ATTENTE', l: 'En attente' },
    { v: 'RECUE_PARTIELLEMENT', l: 'Partielles' },
    { v: 'RECUE', l: 'Reçues' },
    { v: 'ANNULEE', l: 'Annulées' },
  ];

  /** Commande affichée dans le panneau de détail (null = fermé). */
  readonly detail = signal<CommandeFournisseur | null>(null);
  readonly detailChargement = signal(false);

  /** Id de la commande en cours d'action (réceptionner/annuler). */
  readonly actionEnCours = signal<number | null>(null);
  readonly erreurAction = signal<string | null>(null);

  readonly commandesFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const statut = this.filtreStatut();
    return this.commandes().filter((c) => {
      const okStatut = statut === 'TOUS' || c.statut === statut;
      const okRecherche = !q || c.code.toLowerCase().includes(q) || c.fournisseurNom.toLowerCase().includes(q);
      return okStatut && okRecherche;
    });
  });

  readonly nbEnAttente = computed(() => this.commandes().filter((c) => c.statut === 'EN_ATTENTE').length);
  readonly nbPartielles = computed(() => this.commandes().filter((c) => c.statut === 'RECUE_PARTIELLEMENT').length);
  readonly nbRecues = computed(() => this.commandes().filter((c) => c.statut === 'RECUE').length);
  readonly nbAnnulees = computed(() => this.commandes().filter((c) => c.statut === 'ANNULEE').length);

  /**
   * Quantités saisies pour la réception partielle (§3.5), indexées par id de
   * ligne. Initialisées au solde restant quand le détail d'une commande
   * réceptionnable est ouvert ; l'utilisateur ajuste ce qu'il reçoit réellement.
   */
  readonly quantitesPartielles = signal<Record<number, number>>({});

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.commandeService.loadAll().subscribe({
      next: (liste) => {
        this.commandes.set(liste);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les commandes. Le serveur est-il démarré ?');
        this.chargement.set(false);
      },
    });
  }

  ouvrirDetail(c: CommandeFournisseur): void {
    this.detail.set(c);
    this.erreurAction.set(null);
    // Resynchronisation fraîche : une commande peut avoir été réceptionnée ailleurs.
    this.detailChargement.set(true);
    this.commandeService.findById(c.id).subscribe({
      next: (fraiche) => {
        this.detail.set(fraiche);
        this.detailChargement.set(false);
        this.initQuantitesPartielles(fraiche);
      },
      error: () => this.detailChargement.set(false),
    });
  }

  /** Pré-remplit les quantités partielles avec le solde restant de chaque ligne. */
  private initQuantitesPartielles(c: CommandeFournisseur): void {
    const receptive = c.statut === 'EN_ATTENTE' || c.statut === 'RECUE_PARTIELLEMENT';
    if (!receptive) {
      this.quantitesPartielles.set({});
      return;
    }
    const valeurs: Record<number, number> = {};
    for (const l of c.lignes) {
      valeurs[l.id] = this.resteLigne(l);
    }
    this.quantitesPartielles.set(valeurs);
  }

  fermerDetail(): void {
    this.detail.set(null);
  }  receptionner(c: CommandeFournisseur): void {
    if (this.actionEnCours() !== null) return;
    if (!confirm(`Réceptionner la commande ${c.code} en intégralité ?\nLe stock sera incrémenté du solde restant pour chaque ligne.`)) return;
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.receptionner(c.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
        this.initQuantitesPartielles(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? 'Réception refusée (commande déjà reçue ou annulée ?).');
      },
    });
  }

  /**
   * Réception partielle (§3.5) : envoie les quantités saisies, ligne par
   * ligne. Le backend bascule la commande en RECUE si tout est satisfait.
   */
  receptionPartielle(c: CommandeFournisseur): void {
    if (this.actionEnCours() !== null) return;
    const lignes = Object.entries(this.quantitesPartielles())
      .map(([id, qte]) => ({ ligneId: Number(id), quantiteRecue: Number(qte) || 0 }))
      .filter((l) => l.quantiteRecue > 0);
    if (lignes.length === 0) {
      this.erreurAction.set('Saisissez au moins une quantité à réceptionner.');
      return;
    }
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.receptionnerPartiellement(c.id, { lignes }).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
        this.initQuantitesPartielles(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? 'Réception partielle refusée (quantités invalides ?).');
      },
    });
  }

  /** Solde restant à réceptionner sur une ligne. */
  resteLigne(l: LigneCommandeFournisseur): number {
    return Math.max(0, l.quantite - (l.quantiteRecue ?? 0));
  }

  /** Quantité partielle saisie pour une ligne (fallback : solde restant). */
  quantiteSaisie(l: LigneCommandeFournisseur): number {
    const saisie = this.quantitesPartielles()[l.id];
    return saisie === undefined ? this.resteLigne(l) : saisie;
  }

  saisirQuantite(l: LigneCommandeFournisseur, valeur: number): void {
    const borne = Math.min(Math.max(0, Math.floor(valeur) || 0), this.resteLigne(l));
    this.quantitesPartielles.update((m) => ({ ...m, [l.id]: borne }));
  }

  annuler(c: CommandeFournisseur): void {
    if (this.actionEnCours() !== null) return;
    if (!confirm(`Annuler la commande ${c.code} ? Aucun impact sur le stock.`)) return;
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.annuler(c.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? "Annulation impossible (la commande n'est peut-être plus en attente).");
      },
    });
  }

  /** Met à jour la liste (et le détail est rechargé par l'appelant). */
  private remplacer(maj: CommandeFournisseur): void {
    this.commandes.update((liste) => liste.map((c) => (c.id === maj.id ? maj : c)));
  }

  libelleStatut(statut: StatutCommandeFournisseur): string {
    switch (statut) {
      case 'EN_ATTENTE':
        return 'En attente';
      case 'RECUE_PARTIELLEMENT':
        return 'Partiellement reçue';
      case 'RECUE':
        return 'Reçue';
      default:
        return 'Annulée';
    }
  }

  /** Total des unités déjà reçues sur une commande (résumé de progression). */
  totalRecu(c: CommandeFournisseur): number {
    return c.lignes.reduce((n, l) => n + (l.quantiteRecue ?? 0), 0);
  }

  /** Total des unités commandées (résumé de progression). */
  totalCommande(c: CommandeFournisseur): number {
    return c.lignes.reduce((n, l) => n + l.quantite, 0);
  }

  montant(v: number | string | null | undefined): string {
    const n = Number(v ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }

  dateCourte(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  heure(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
