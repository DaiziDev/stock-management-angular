package com.sgs.backend.entreprise.dto;

import jakarta.validation.constraints.Email;

/**
 * Charge utile de PUT /api/entreprises/{id} : les coordonnées modifiables
 * d'une entreprise cliente -- SANS le nom.
 *
 * Pourquoi pas EntrepriseRequestDTO : son @NotBlank sur `nom` rejetait
 * (400) chaque requête du formulaire d'édition, qui n'envoie pas le nom
 * (non modifiable, identifiant de cloisonnement RG-10). Et forcer le
 * client à renvoyer le nom l'aurait rendu modifiable par effet de bord.
 */
public record EntrepriseUpdateDTO(
        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        @Email(message = "l'adresse mail doit être valide")
        String mail,

        String numTel
) {}
