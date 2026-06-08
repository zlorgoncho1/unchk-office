package sn.unchk.office.common.authz;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

/**
 * Configuration d'autorisation OPA pour les services métier.
 * <p>
 * Câble le {@link RestClient} pointant vers OPA, le {@link OpaClient} et l'aspect
 * {@link ResourceAccessGuard} (anti-IDOR). Active la prise en compte des
 * {@link OpaProprietes}.
 */
@Configuration
@EnableConfigurationProperties(OpaProprietes.class)
public class ConfigurationAutorisationOpa {

    /**
     * Client HTTP synchrone (servlet) préconfiguré avec l'URL de base d'OPA.
     */
    @Bean
    public RestClient restClientOpa(OpaProprietes proprietes) {
        return RestClient.builder()
                .baseUrl(proprietes.url())
                .build();
    }

    /**
     * Client d'autorisation interrogeant la règle {@code allow} d'OPA.
     */
    @Bean
    public OpaClient opaClient(RestClient restClientOpa, OpaProprietes proprietes) {
        return new OpaClient(restClientOpa, proprietes);
    }

    /**
     * Aspect anti-IDOR déclenché par {@link VerifieAccesObjet}.
     * Le fournisseur d'attributs ABAC est optionnel : injecté seulement si le service en déclare un.
     */
    @Bean
    public ResourceAccessGuard resourceAccessGuard(
            OpaClient opaClient,
            ObjectProvider<FournisseurAttributsRessource> fournisseur) {
        return new ResourceAccessGuard(opaClient, fournisseur);
    }
}
