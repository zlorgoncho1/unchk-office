package sn.unchk.office.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du validateur d'audience exposé par {@link ConfigurationServeurRessources}.
 * Vérifie qu'un jeton est accepté seulement si l'audience attendue figure dans "aud".
 */
class ValidateurAudienceTest {

    /** Construit un JWT de test avec une liste d'audiences. */
    private Jwt jwtAvecAudience(List<String> audiences) {
        return Jwt.withTokenValue("jeton")
                .header("alg", "RS256")
                .subject("u-1")
                .audience(audiences)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void accepte_quand_l_audience_attendue_est_presente() {
        OAuth2TokenValidator<Jwt> validateur =
                ConfigurationServeurRessources.validateurAudience("unchk-office");

        // Quand le jeton contient bien l'audience attendue
        OAuth2TokenValidatorResult resultat =
                validateur.validate(jwtAvecAudience(List.of("unchk-office", "autre")));

        // Alors la validation réussit
        assertThat(resultat.hasErrors()).isFalse();
    }

    @Test
    void rejette_quand_l_audience_attendue_est_absente() {
        OAuth2TokenValidator<Jwt> validateur =
                ConfigurationServeurRessources.validateurAudience("unchk-office");

        // Quand le jeton ne contient pas l'audience attendue
        OAuth2TokenValidatorResult resultat =
                validateur.validate(jwtAvecAudience(List.of("autre-service")));

        // Alors la validation échoue
        assertThat(resultat.hasErrors()).isTrue();
    }
}
