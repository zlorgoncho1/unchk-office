package sn.unchk.office.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sn.unchk.office.common.audit.ConfigurationAudit;
import sn.unchk.office.common.authz.ConfigurationAutorisationOpa;
import sn.unchk.office.common.export.ConfigurationExport;
import sn.unchk.office.common.messaging.KafkaConsumerConfig;
import sn.unchk.office.common.messaging.KafkaProducerConfig;
import sn.unchk.office.common.web.ConfigurationWeb;
import sn.unchk.office.common.web.GlobalExceptionHandler;

/**
 * Importe les briques transverses de {@code libs/common} nécessaires à l'identity-service,
 * À L'EXCEPTION de la configuration de sécurité générique (serveur de ressources).
 * <p>
 * En effet, l'identity-service fournit sa propre chaîne de sécurité
 * ({@link sn.unchk.office.identity.securite.ConfigurationSecuriteIdentity}) pour ouvrir ses
 * endpoints d'authentification et son JWKS. L'auto-configuration complète de {@code common}
 * ({@code CommonAutoConfiguration}) est donc exclue dans {@code application.yml}, et on réimporte
 * ici uniquement : autorisation OPA (ABAC anti-IDOR), gestion d'erreurs/correlation web,
 * messagerie Kafka (producteur + consommateur), export PDF/Excel et audit.
 */
@Configuration
@Import({
        ConfigurationAutorisationOpa.class,
        ConfigurationWeb.class,
        GlobalExceptionHandler.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        ConfigurationExport.class,
        ConfigurationAudit.class
})
public class ConfigurationCommuneIdentity {
}
