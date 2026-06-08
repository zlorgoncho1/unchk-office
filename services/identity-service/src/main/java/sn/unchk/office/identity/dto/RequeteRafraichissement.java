package sn.unchk.office.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête de rafraîchissement ({@code POST /api/identity/auth/refresh}).
 *
 * @param refreshToken jeton de rafraîchissement émis lors de la connexion
 */
public record RequeteRafraichissement(

        @NotBlank(message = "Le refresh token est obligatoire.")
        String refreshToken
) {
}
