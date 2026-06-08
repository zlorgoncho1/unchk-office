package sn.unchk.office.document.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

/**
 * Déclaration explicite du topic produit par ce service.
 * <p>
 * L'auto-création de topics est désactivée côté Kafka (KRaft) : chaque service crée les
 * siens via {@link NewTopic}. Le topic {@code document.documents} suit une politique
 * {@code delete} (cf. architecture.md) avec 3 partitions ; la clé de partition est
 * l'identifiant du document.
 */
@Configuration
public class ConfigurationTopics {

    /** Nombre de partitions par défaut (architecture : 3 sauf notifications). */
    private static final int PARTITIONS = 3;

    /** Réplication mono-broker en développement. */
    private static final short REPLICATION = 1;

    @Bean
    public NewTopic topicDocuments() {
        return TopicBuilder.name(Topics.DOCUMENT_DOCUMENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                // Politique de rétention "delete" (pas de compaction pour les documents).
                .config("cleanup.policy", "delete")
                .build();
    }
}
