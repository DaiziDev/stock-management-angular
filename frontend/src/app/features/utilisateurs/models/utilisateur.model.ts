import type { UserRole } from '../../../core/models/models';

/**
 * Modèles de la gestion des comptes utilisateurs (ADMIN d'entreprise).
 *
 * Correspondances backend :
 * - UtilisateurResponseDTO  → GET/PUT /api/utilisateurs (+ /{id}) ;
 * - RegisterRequest         → POST /api/auth/register (création, avec
 *   mot de passe + entrepriseId -- le backend le refuse si l'ADMIN ne
 *   crée pas dans SA propre entreprise) ;
 * - UtilisateurUpdateDTO    → PUT /api/utilisateurs/{id} (ni login ni
 *   mot de passe : opérations sensibles hors périmètre de cette page).
 */
export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  login: string;
  mail: string | null;
  numTel: string | null;
  role: UserRole;
}

/** Payload de création : passe par POST /api/auth/register. */
export interface UtilisateurCreateRequest {
  nom: string;
  prenom: string;
  login: string;
  motDePasse: string;
  mail: string | null;
  numTel: string | null;
  role: UserRole;
  /** Récupéré de la session -- jamais saisi ni modifiable dans le formulaire. */
  entrepriseId: number;
}

/** Payload d'édition : passe par PUT /api/utilisateurs/{id} (sans mot de passe). */
export interface UtilisateurUpdateRequest {
  nom: string;
  prenom: string;
  mail: string | null;
  numTel: string | null;
  role: UserRole;
}
