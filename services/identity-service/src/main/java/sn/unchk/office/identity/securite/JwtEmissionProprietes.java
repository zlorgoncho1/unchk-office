package sn.unchk.office.identity.securite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés d'émission des JWT par l'identity-service.
 * <p>
 * Renseignées sous le préfixe {@code unchk.identity.jwt} dans {@code application.yml}.
 * L'{@code issuer} et l'{@code audience} doivent correspondre à ce que valident le gateway
 * et les services métier (cf. {@code unchk.security.jwt} côté consommateurs).
 *
 * @param issuer        émetteur déclaré (claim {@code iss})
 * @param audience      audience déclarée (claim {@code aud})
 * @param accessTtlMin  durée de vie de l'access token en minutes
 * @param refreshTtlDays durée de vie du refresh token en jours
 */
@ConfigurationProperties(prefix = "unchk.identity.jwt")
public record JwtEmissionProprietes(
        String issuer,
        String audience,
        Integer accessTtlMin,
        Integer refreshTtlDays
) {

    public JwtEmissionProprietes {
        // Valeurs de repli prudentes si la configuration est incomplète.
        if (issuer == null || issuer.isBlank()) {
            issuer = "unchk-office";
        }
        if (audience == null || audience.isBlank()) {
            audience = "unchk-office";
        }
        if (accessTtlMin == null || accessTtlMin <= 0) {
            accessTtlMin = 30;
        }
        if (refreshTtlDays == null || refreshTtlDays <= 0) {
            refreshTtlDays = 7;
        }
    }
}
