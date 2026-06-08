package sn.unchk.office.common.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration de base d'un consommateur Kafka (désérialisation JSON).
 * <p>
 * Chaque service consomme les topics des autres pour alimenter ses read-models locaux
 * (projections CQRS). La désérialisation JSON est encapsulée dans un
 * {@link ErrorHandlingDeserializer} : un message illisible n'interrompt pas le flux,
 * il est traité comme une erreur récupérable plutôt que de bloquer la partition.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    /** Adresse(s) du/des broker(s) Kafka, fournie par la configuration du service. */
    @Value("${spring.kafka.bootstrap-servers:kafka:19092}")
    private String bootstrapServers;

    /** Identifiant du groupe de consommation, propre à chaque service. */
    @Value("${spring.kafka.consumer.group-id:unchk-service}")
    private String groupId;

    /**
     * Fabrique de consommateurs : clé String, valeur JSON désérialisée en {@link DomainEvent}.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // Première lecture : on repart du plus ancien pour ne rien manquer lors d'un nouveau service.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Désérialiseurs robustes : la clé reste String, la valeur passe par la gestion d'erreur.
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // On fait confiance aux types du paquet commun et des services pour la désérialisation.
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "sn.unchk.office.*");
        // Sans en-tête de type, on désérialise par défaut vers l'enveloppe DomainEvent.
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DomainEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Fabrique de conteneurs d'écoute utilisée par les annotations {@code @KafkaListener}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
