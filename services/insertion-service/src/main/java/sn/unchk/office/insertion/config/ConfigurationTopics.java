package sn.unchk.office.insertion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import sn.unchk.office.common.messaging.Topics;

/**
 * Déclaration explicite du topic Kafka produit par l'insertion-service.
 * <p>
 * L'auto-création de topics est désactivée sur le broker ({@code auto.create.topics=false}) :
 * chaque service crée donc les topics dont il est producteur via un bean {@link NewTopic}.
 * Sans cette déclaration, {@code kafkaTemplate.send(insertion.events, ...)} bloque jusqu'à
 * {@code max.block.ms} (métadonnées introuvables : {@code UNKNOWN_TOPIC_OR_PARTITION}) puis lève
 * une {@code TimeoutException} — ce qui faisait échouer en 500 toute écriture d'insertion
 * (création de partenaire, stage, contact, situation). Bug révélé par la recette vidéo
 * (le seed étant chargé en SQL, il ne passait pas par le producteur).
 * <p>
 * {@code insertion.events} est un flux d'événements (et non un transfert d'état par clé) :
 * politique de rétention par défaut ({@code delete}), partitionnement par UUID d'étudiant,
 * réplication 1 (broker unique de développement).
 */
@Configuration
public class ConfigurationTopics {

    /** Topic des événements d'insertion (stages, partenaires, contacts, situations). */
    @Bean
    public NewTopic topicInsertionEvents() {
        return TopicBuilder.name(Topics.INSERTION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
