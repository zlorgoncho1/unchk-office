package sn.unchk.office.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête de connexion ({@code POST /api/identity/auth/login}).
 *
 * @param email       courriel (login)
 * @param motDePasse  mot de passe en clair (transporté sur TLS, jamais journalisé)
 */
public record RequeteConnexion(

        @NotBlank(message = "Le courriel est obligatoire.")
        @Email(message = "Le courriel doit être valide.")
        @Size(max = 320, message = "Le courriel est trop long.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 200, message = "Le mot de passe doit comporter entre 8 et 200 caractères.")
        String motDePasse
) {
}
