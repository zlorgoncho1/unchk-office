package sn.unchk.office.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du document-service.
 * <p>
 * Service de gestion documentaire (courrier, notes de service, notes administratives,
 * circulaires). Les métadonnées vivent dans PostgreSQL, le binaire dans MinIO. La
 * communication inter-services est 100% Kafka (topic produit : {@code document.documents}).
 * <p>
 * {@code @EnableScheduling} active le relais Outbox (publication différée vers Kafka).
 */
@SpringBootApplication
@EnableScheduling
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
