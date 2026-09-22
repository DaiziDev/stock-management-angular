/** Entreprise cliente vue depuis la console plateforme. */
export interface EntrepriseCliente {
  id: number;
  nom: string;
  adresse1: string | null;
  adresse2: string | null;
  ville: string | null;
  codePostal: string | null;
  pays: string | null;
  mail: string | null;
  numTel: string | null;
  nbUtilisateurs: number;
}

/** Charge utile de l'onboarding : entreprise + son premier ADMIN. */
export interface OnboardingRequest {
  nomEntreprise: string;
  adresse1: string | null;
  adresse2: string | null;
  ville: string | null;
  codePostal: string | null;
  pays: string | null;
  mailEntreprise: string | null;
  numTelEntreprise: string | null;
  adminPrenom: string;
  adminNom: string;
  adminLogin: string;
  adminMotDePasse: string;
  adminMail: string | null;
  adminNumTel: string | null;
}

/** Résumé compact d'une entreprise récemment onboardée. */
export interface EntrepriseRecente {
  id: number;
  nom: string;
  mail: string | null;
  ville: string | null;
  nbUtilisateurs: number;
  createdAt: string;
}

/** Statistiques globales de la plateforme (toutes entreprises confondues). */
export interface PlateformeStats {
  // ── Parc ──
  nbEntreprises: number;
  nbUtilisateurs: number;
  nbAdmins: number;

  // ── Activité agrégée du parc ──
  nbClients: number;
  nbArticles: number;
  nbVentes: number;
  nbVentesDuMois: number;
  /** CA TTC du mois, en unités monétaires (string côté JSON pour les BigDecimal). */
  chiffreAffairesDuMois: string;
  nbArticlesEnAlerte: number;
  nbCommandesEnCours: number;

  // ── Croissance ──
  nbNouvellesEntreprisesDuMois: number;
  dernieresEntreprises: EntrepriseRecente[];
}
