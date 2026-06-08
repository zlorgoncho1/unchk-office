package sn.unchk.office.communication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

/**
 * Déclaration des topics Kafka possédés par communication-service.
 * <p>
 * L'auto-création est désactivée côté broker ({@code KAFKA_AUTO_CREATE_TOPICS_ENABLE=false}) :
 * on déclare donc explicitement les topics dont ce service est producteur. Spring Kafka les crée
 * au démarrage via l'AdminClient s'ils n'existent pas. La politique de rétention/compactage est
 * conforme à l'architecture (topics d'événements en {@code delete}, {@code notifications} en
 * fan-out à 6 partitions).
 */
@Configuration
public class ConfigurationTopicsKafka {

    /** Comptes rendus (déclencheur de notifications). Politique delete, 3 partitions. */
    @Bean
    public NewTopic topicComptesRendus() {
        return TopicBuilder.name(Topics.COMMUNICATION_COMPTESRENDUS)
                .partitions(3)
                .replicas(1)
                .config("cleanup.policy", "delete")
                .build();
    }

    /** Réunions planifiées. Politique delete, 3 partitions. */
    @Bean
    public NewTopic topicReunions() {
        return TopicBuilder.name(Topics.COMMUNICATION_REUNIONS)
                .partitions(3)
                .replicas(1)
                .config("cleanup.policy", "delete")
                .build();
    }

    /** Notifications poussées (fan-out). Politique delete, 6 partitions (clé = recipientId). */
    @Bean
    public NewTopic topicNotifications() {
        return TopicBuilder.name(Topics.NOTIFICATIONS)
                .partitions(6)
                .replicas(1)
                .config("cleanup.policy", "delete")
                .build();
    }
}
