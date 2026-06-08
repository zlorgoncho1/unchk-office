package sn.unchk.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de réinitialisation de mot de passe ({@code PUT /api/identity/users/{id}/password}),
 * réservé à l'admin.
 *
 * @param nouveauMotDePasse nouveau mot de passe (haché en BCrypt côté serveur)
 */
public record RequeteChangementMotDePasse(

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 8, max = 200, message = "Le mot de passe doit comporter entre 8 et 200 caractères.")
        String nouveauMotDePasse
) {
}
