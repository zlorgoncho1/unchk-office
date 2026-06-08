package sn.unchk.office.common.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration web transverse : enregistre les filtres communs et le gestionnaire d'erreurs.
 * <p>
 * Le {@link GlobalExceptionHandler} est détecté par {@code @RestControllerAdvice}. Ici on
 * déclare l'ordre d'exécution des filtres (corrélation d'abord, puis en-têtes de sécurité).
 */
@Configuration
public class ConfigurationWeb {

    /**
     * Enregistre le filtre d'identifiant de corrélation en tout premier.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> enregistrementCorrelation() {
        FilterRegistrationBean<CorrelationIdFilter> enregistrement =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        enregistrement.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return enregistrement;
    }

    /**
     * Enregistre le filtre d'en-têtes de sécurité juste après la corrélation.
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> enregistrementEntetesSecurite() {
        FilterRegistrationBean<SecurityHeadersFilter> enregistrement =
                new FilterRegistrationBean<>(new SecurityHeadersFilter());
        enregistrement.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return enregistrement;
    }
}
