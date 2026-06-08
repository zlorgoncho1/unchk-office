package sn.unchk.office.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import sn.unchk.office.common.audit.ConfigurationAudit;
import sn.unchk.office.common.authz.ConfigurationAutorisationOpa;
import sn.unchk.office.common.export.ConfigurationExport;
import sn.unchk.office.common.messaging.KafkaConsumerConfig;
import sn.unchk.office.common.messaging.KafkaProducerConfig;
import sn.unchk.office.common.security.ConfigurationServeurRessources;
import sn.unchk.office.common.web.ConfigurationWeb;

/**
 * Auto-configuration de la librairie commune.
 * <p>
 * Importée automatiquement par tout microservice métier qui dépend de {@code common}
 * (via {@code META-INF/spring/...AutoConfiguration.imports}). Elle agrège toutes les
 * briques transverses : sécurité JWT/JWKS, autorisation OPA anti-IDOR, gestion d'erreurs
 * web, messagerie Kafka, export PDF/Excel et audit.
 * <p>
 * Le gateway réactif (WebFlux) ne dépend PAS de cette librairie : elle est purement servlet.
 */
@AutoConfiguration
@Import({
        ConfigurationServeurRessources.class,
        ConfigurationAutorisationOpa.class,
        ConfigurationWeb.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        ConfigurationExport.class,
        ConfigurationAudit.class
})
public class CommonAutoConfiguration {
}
