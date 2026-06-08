package sn.unchk.office.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sn.unchk.office.gateway.filter.CorrelationIdFilter;
import sn.unchk.office.gateway.filter.OpaAuthorizationFilter;
import sn.unchk.office.gateway.filter.RateLimitFilter;
import sn.unchk.office.gateway.filter.SecurityHeadersFilter;
import sn.unchk.office.gateway.security.OpaClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de chargement du contexte de l'API Gateway.
 *
 * <p>Vérifie que le contexte Spring démarre correctement et que les filtres de sécurité
 * essentiels (corrélation, en-têtes, rate-limiting, autorisation OPA) ainsi que le client OPA
 * sont bien enregistrés comme beans.</p>
 *
 * <p>La configuration de test ({@code src/test/resources/application.yml}) évite tout appel
 * réseau au démarrage (JWKS différé, OPA non sollicité).</p>
 */
@SpringBootTest
@DisplayName("API Gateway — chargement du contexte et présence des filtres de sécurité")
class ApiGatewayApplicationTests {

    @Autowired
    private OpaClient opaClient;

    @Autowired
    private OpaAuthorizationFilter opaAuthorizationFilter;

    @Autowired
    private SecurityHeadersFilter securityHeadersFilter;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Test
    @DisplayName("Le contexte se charge et tous les filtres de sécurité sont présents")
    void leContexteSeChargeAvecLesFiltres() {
        // Le client OPA et les filtres globaux doivent être disponibles.
        assertThat(opaClient).isNotNull();
        assertThat(opaAuthorizationFilter).isNotNull();
        assertThat(securityHeadersFilter).isNotNull();
        assertThat(correlationIdFilter).isNotNull();
        assertThat(rateLimitFilter).isNotNull();
    }
}
