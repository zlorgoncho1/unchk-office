package sn.unchk.office.admin.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

import java.util.Map;

/**
 * Déclaration explicite du topic produit par admin-service.
 * <p>
 * L'auto-création est désactivée côté broker ({@code KAFKA_AUTO_CREATE_TOPICS_ENABLE=false}) :
 * chaque service déclare ses propres topics via {@link NewTopic}. Le topic {@code admin.budget}
 * est compacté (dernier état par clé d'agrégat) avec 3 partitions et une réplication de 1
 * (mono-broker de développement), conformément à docs/architecture.md.
 */
@Configuration
public class ConfigurationTopicsKafka {

    /** Nombre de partitions du topic budget. */
    private static final int PARTITIONS = 3;

    /** Facteur de réplication (mono-broker de dev). */
    private static final short REPLICATION = 1;

    /**
     * Topic d'état du budget : politique de compactage (on conserve le dernier état par clé).
     */
    @Bean
    public NewTopic topicAdminBudget() {
        return TopicBuilder.name(Topics.ADMIN_BUDGET)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .configs(Map.of("cleanup.policy", "compact"))
                .build();
    }
}
