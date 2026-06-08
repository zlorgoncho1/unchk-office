package sn.unchk.office.academic.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

import java.util.Map;

/**
 * Déclaration explicite des topics Kafka produits par le academic-service.
 * <p>
 * L'auto-création de topics est désactivée sur le broker ({@code auto.create.topics=false}) :
 * chaque service crée donc les topics dont il est producteur via un bean {@link NewTopic}.
 * Le academic-service ne produit que {@code academic.formations}.
 * <p>
 * Politique {@code compact} (transfert d'état par clé = dernier état de la formation),
 * partitionnement par {@code formationId}, réplication 1 (broker unique de développement).
 */
@Configuration
public class ConfigurationTopics {

    /**
     * Topic d'état des formations : dernier état connu par formation (clé = formationId).
     * Politique de compaction pour permettre la reconstruction d'une projection depuis l'offset 0.
     */
    @Bean
    public NewTopic topicFormations() {
        return TopicBuilder.name(Topics.ACADEMIC_FORMATIONS)
                .partitions(3)
                .replicas(1)
                .config("cleanup.policy", "compact")
                .build();
    }
}
