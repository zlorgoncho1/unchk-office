package sn.unchk.office.common.messaging;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration de base d'un producteur Kafka (sérialisation JSON).
 * <p>
 * La clé du message est une chaîne (souvent l'UUID de l'agrégat, pour le partitionnement),
 * la valeur est un {@link DomainEvent} sérialisé en JSON. Les producteurs sont idempotents
 * et confirmés par tous les réplicas ({@code acks=all}) pour ne pas perdre d'événement.
 */
@Configuration
public class KafkaProducerConfig {

    /** Adresse(s) du/des broker(s) Kafka, fournie par la configuration du service. */
    @Value("${spring.kafka.bootstrap-servers:kafka:19092}")
    private String bootstrapServers;

    /**
     * Fabrique de producteurs : clé String, valeur JSON.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Idempotence : pas de doublon en cas de réémission interne du client.
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Durabilité maximale : on attend l'accusé de tous les réplicas en phase.
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Gabarit d'envoi prêt à l'emploi pour publier des événements.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
