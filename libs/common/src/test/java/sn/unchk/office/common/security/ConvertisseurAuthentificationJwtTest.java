package sn.unchk.office.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du convertisseur d'authentification {@link ConvertisseurAuthentificationJwt}.
 * Vérifie l'extraction de l'identifiant (sub) et des rôles (claim "roles").
 */
class ConvertisseurAuthentificationJwtTest {

    private final ConvertisseurAuthentificationJwt convertisseur = new ConvertisseurAuthentificationJwt();

    /** Construit un JWT de test avec un sujet et une liste de rôles. */
    private Jwt jwtAvecRoles(String sub, List<String> roles) {
        return Jwt.withTokenValue("jeton-factice")
                .header("alg", "RS256")
                .subject(sub)
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void extrait_le_userId_depuis_le_claim_sub() {
        // Étant donné un JWT dont le sujet est l'identifiant utilisateur
        Jwt jwt = jwtAvecRoles("u-123", List.of("enseignant"));

        // Quand on convertit
        AbstractAuthenticationToken token = convertisseur.convert(jwt);

        // Alors le nom du principal est le "sub"
        assertThat(token.getName()).isEqualTo("u-123");
    }

    @Test
    void transforme_les_roles_en_autorites_prefixees() {
        // Étant donné un JWT portant deux rôles
        Jwt jwt = jwtAvecRoles("u-1", List.of("enseignant", "admin"));

        // Quand on convertit
        AbstractAuthenticationToken token = convertisseur.convert(jwt);

        // Alors chaque rôle devient une autorité préfixée ROLE_
        assertThat(token.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_enseignant", "ROLE_admin");
    }

    @Test
    void tolere_l_absence_de_roles() {
        // Étant donné un JWT sans claim "roles"
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "RS256")
                .subject("u-2")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        // Quand on convertit
        AbstractAuthenticationToken token = convertisseur.convert(jwt);

        // Alors il n'y a aucune autorité, sans erreur
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("u-2");
    }
}
