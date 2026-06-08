package sn.unchk.office.common.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de l'audit : expose le {@link AuditLogger} comme bean partagé.
 */
@Configuration
public class ConfigurationAudit {

    @Bean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }
}
