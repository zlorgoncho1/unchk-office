package sn.unchk.office.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du serveur de ressources JWT.
 * <p>
 * Renseignées dans {@code application.yml} sous le préfixe {@code unchk.security.jwt}.
 * Elles permettent de valider les jetons émis par l'identity-service maison
 * (issuer attendu, audience attendue et URL du jeu de clés publiques JWKS).
 *
 * @param jwksUri  URL exposant les clés publiques (JWKS) servant à vérifier la signature RS256
 * @param issuer   émetteur attendu du jeton (claim {@code iss})
 * @param audience audience attendue du jeton (claim {@code aud})
 */
@ConfigurationProperties(prefix = "unchk.security.jwt")
public record JwtProprietes(
        String jwksUri,
        String issuer,
        String audience
) {
}
