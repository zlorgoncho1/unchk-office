package sn.unchk.office.identity.dto;

import java.util.List;

/**
 * Réponse d'authentification : couple access token + refresh token et métadonnées.
 *
 * @param accessToken  JWT RS256 à présenter dans l'en-tête Authorization
 * @param refreshToken jeton de rafraîchissement (à conserver côté client de façon sûre)
 * @param tokenType    type de jeton (toujours {@code Bearer})
 * @param expiresIn    durée de vie de l'access token en secondes
 * @param userId       identifiant de l'utilisateur authentifié
 * @param roles        rôles de l'utilisateur
 */
public record ReponseJetons(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String userId,
        List<String> roles
) {

    /** Fabrique une réponse de type Bearer. */
    public static ReponseJetons bearer(String accessToken, String refreshToken,
                                       long expiresIn, String userId, List<String> roles) {
        return new ReponseJetons(accessToken, refreshToken, "Bearer", expiresIn, userId, roles);
    }
}
