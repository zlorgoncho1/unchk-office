package sn.unchk.office.people.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

import java.util.Map;

/**
 * Declaration explicite des topics produits par people-service.
 * <p>
 * L'auto-creation est desactivee cote broker ({@code KAFKA_AUTO_CREATE_TOPICS_ENABLE=false}) :
 * chaque service cree ses propres topics via {@link NewTopic}. people-service est
 * proprietaire de {@code people.students} et {@code people.staff}.
 * <p>
 * Politique {@code compact} (etat par cle) : les nouveaux consommateurs rejouent depuis
 * l'offset 0 pour reconstruire leur projection (dernier etat par cle). Replication 1
 * (mono-broker de dev), 3 partitions (cf. docs/architecture.md).
 */
@Configuration
public class KafkaTopicsConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    /** Topic canonique des etudiants (compacte sur studentId). */
    @Bean
    public NewTopic topicPeopleStudents() {
        return TopicBuilder.name(Topics.PEOPLE_STUDENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .configs(Map.of("cleanup.policy", "compact"))
                .build();
    }

    /** Topic canonique du personnel / formateurs (compacte sur staffId). */
    @Bean
    public NewTopic topicPeopleStaff() {
        return TopicBuilder.name(Topics.PEOPLE_STAFF)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .configs(Map.of("cleanup.policy", "compact"))
                .build();
    }
}
