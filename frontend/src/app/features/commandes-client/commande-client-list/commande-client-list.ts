import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommandeClientService } from '../services/commande-client-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import { type CommandeClient, type StatutCommandeClient } from '../models/commande-client.model';

/**
 * Liste des commandes client : filtres par statut + recherche, tableau
 * desktop / cartes mobile, panneau de détail (lignes, totaux) et actions
 * de cycle de vie — Valider génère les sorties de stock côté backend
 * (409 en bloc si stock insuffisant), Annuler ne touche pas au stock.
 */
@Component({
  selector: 'app-commande-client-list',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './commande-client-list.html',
})
export class CommandeClientList implements OnInit {
  private readonly commandeService = inject(CommandeClientService);

  readonly commandes = signal<CommandeClient[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly recherche = signal('');
  readonly filtreStatut = signal<'TOUS' | StatutCommandeClient>('TOUS');

  /** Choix de filtres typés (le template ne peut pas typer un littéral inline). */
  readonly filtres: { v: 'TOUS' | StatutCommandeClient; l: string }[] = [
    { v: 'TOUS', l: 'Toutes' },
    { v: 'EN_COURS', l: 'En cours' },
    { v: 'VALIDEE', l: 'Validées' },
    { v: 'EXPEDIEE', l: 'Expédiées' },
    { v: 'LIVREE', l: 'Livrées' },
    { v: 'ANNULEE', l: 'Annulées' },
  ];

  /** Commande affichée dans le panneau de détail (null = fermé). */
  readonly detail = signal<CommandeClient | null>(null);
  readonly detailChargement = signal(false);

  /** Id de la commande en cours d'action (valider/annuler) — 1 seul à la fois. */
  readonly actionEnCours = signal<number | null>(null);
  readonly erreurAction = signal<string | null>(null);

  readonly commandesFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const statut = this.filtreStatut();
    return this.commandes().filter((c) => {
      const okStatut = statut === 'TOUS' || c.statut === statut;
      const okRecherche =
        !q ||
        c.code.toLowerCase().includes(q) ||
        c.clientNom.toLowerCase().includes(q);
      return okStatut && okRecherche;
    });
  });

  readonly nbEnCours = computed(() => this.commandes().filter((c) => c.statut === 'EN_COURS').length);
  readonly nbValidees = computed(() => this.commandes().filter((c) => c.statut === 'VALIDEE').length);
  readonly nbExpediees = computed(() => this.commandes().filter((c) => c.statut === 'EXPEDIEE').length);
  readonly nbLivrees = computed(() => this.commandes().filter((c) => c.statut === 'LIVREE').length);
  readonly nbAnnulees = computed(() => this.commandes().filter((c) => c.statut === 'ANNULEE').length);
  /** Commandes en cours d'acheminement : validées (stock déjà sorti), expédiées ou livrées. */
  readonly nbEnLivraison = computed(() => this.nbValidees() + this.nbExpediees() + this.nbLivrees());

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

  ouvrirDetail(c: CommandeClient): void {
    this.detail.set(c);
    this.erreurAction.set(null);
    // Resynchronisation fraîche : une commande peut avoir été validée ailleurs.
    this.detailChargement.set(true);
    this.commandeService.findById(c.id).subscribe({
      next: (fraiche) => {
        this.detail.set(fraiche);
        this.detailChargement.set(false);
      },
      error: () => (this.detailChargement.set(false)),
    });
  }

  fermerDetail(): void {
    this.detail.set(null);
  }

  valider(c: CommandeClient): void {
    if (this.actionEnCours() !== null) return;
    if (!confirm(`Valider la commande ${c.code} ?\nLe stock sera décrémenté pour chaque ligne.`)) return;
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.valider(c.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? 'Validation refusée : stock insuffisant ou erreur serveur.');
      },
    });
  }

  annuler(c: CommandeClient): void {
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
        this.erreurAction.set(msg ?? "Annulation impossible (la commande n'est peut-être plus en cours).");
      },
    });
  }

  /** VALIDEE -> EXPEDIEE : marchandise remise au transporteur, aucun impact stock. */
  expedier(c: CommandeClient): void {
    if (this.actionEnCours() !== null) return;
    if (!confirm(`Marquer la commande ${c.code} comme expédiée ?`)) return;
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.expedier(c.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? "Expédition impossible (la commande doit être validée).");
      },
    });
  }

  /** EXPEDIEE -> LIVREE (raccourci toléré depuis VALIDEE) : livraison confirmée. */
  livrer(c: CommandeClient): void {
    if (this.actionEnCours() !== null) return;
    if (!confirm(`Marquer la commande ${c.code} comme livrée ?`)) return;
    this.actionEnCours.set(c.id);
    this.erreurAction.set(null);
    this.commandeService.livrer(c.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.actionEnCours.set(null);
        this.detail.set(maj);
      },
      error: (err) => {
        this.actionEnCours.set(null);
        const msg = (err?.error?.message as string) ?? null;
        this.erreurAction.set(msg ?? 'Livraison impossible (la commande doit être validée ou expédiée).');
      },
    });
  }

  /** Met à jour la liste ET le panneau de détail avec la version fraîche. */
  private remplacer(maj: CommandeClient): void {
    this.commandes.update((liste) => liste.map((c) => (c.id === maj.id ? maj : c)));
  }

  libelleStatut(statut: StatutCommandeClient): string {
    switch (statut) {
      case 'EN_COURS':
        return 'En cours';
      case 'VALIDEE':
        return 'Validée';
      case 'EXPEDIEE':
        return 'Expédiée';
      case 'LIVREE':
        return 'Livrée';
      default:
        return 'Annulée';
    }
  }

  /** Progression logistique (0-3) pour la barre du panneau détail. */
  etapeLivraison(statut: StatutCommandeClient): number {
    switch (statut) {
      case 'VALIDEE':
        return 1;
      case 'EXPEDIEE':
        return 2;
      case 'LIVREE':
        return 3;
      default:
        return 0;
    }
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
