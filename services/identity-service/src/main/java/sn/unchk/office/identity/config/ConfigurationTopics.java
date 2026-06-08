package sn.unchk.office.identity.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

import java.util.Map;

/**
 * Déclaration explicite du topic produit par identity-service.
 * <p>
 * L'auto-création est désactivée côté broker ({@code KAFKA_AUTO_CREATE_TOPICS_ENABLE=false}) :
 * chaque service crée donc ses propres topics via {@link NewTopic}. identity-service est
 * propriétaire de {@code identity.users} (cycle de vie des comptes).
 * <p>
 * Politique {@code compact} (état par clé = userId) : un nouveau consommateur rejoue depuis
 * l'offset 0 pour reconstruire sa projection des utilisateurs (dernier état par compte).
 * Réplication 1 (mono-broker de dev), 3 partitions (cf. docs/architecture.md).
 */
@Configuration
public class ConfigurationTopics {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    /** Topic du cycle de vie des comptes utilisateurs (compacté sur userId). */
    @Bean
    public NewTopic topicIdentityUsers() {
        return TopicBuilder.name(Topics.IDENTITY_USERS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .configs(Map.of("cleanup.policy", "compact"))
                .build();
    }
}
