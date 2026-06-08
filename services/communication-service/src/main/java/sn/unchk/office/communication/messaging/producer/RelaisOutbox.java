package sn.unchk.office.communication.messaging.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.domain.OutboxMessage;
import sn.unchk.office.communication.repository.OutboxRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Relais de l'Outbox vers Kafka.
 * <p>
 * Balaye périodiquement les messages non publiés, les émet sur leur topic avec l'enveloppe
 * {@link DomainEvent} (valeur) et les en-têtes Kafka standard (eventId, eventType,
 * eventVersion, aggregateType, aggregateId, occurredAt, traceId, producer), puis les marque
 * comme publiés. La valeur du message ne porte que l'état de l'agrégat (JSON), conformément
 * à l'architecture (enveloppe dans les en-têtes).
 */
@Component
public class RelaisOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelaisOutbox.class);

    /** Nom du service producteur (en-tête {@code producer}). */
    private static final String PRODUCTEUR = "communication-service";
    /** Version du schéma d'événement (évolutions additives uniquement). */
    private static final String VERSION_EVENT = "1";
    /** Nombre maximal de messages relayés par passage. */
    private static final int TAILLE_LOT = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RelaisOutbox(OutboxRepository outboxRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publie les messages en attente. Exécuté à intervalle régulier (toutes les secondes).
     * La transaction couvre le marquage « publié » pour ne pas réémettre indéfiniment.
     */
    @Scheduled(fixedDelayString = "${communication.outbox.relais-ms:1000}")
    @Transactional
    public void relayer() {
        List<OutboxMessage> enAttente =
                outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, TAILLE_LOT));
        if (enAttente.isEmpty()) {
            return;
        }
        for (OutboxMessage message : enAttente) {
            try {
                publier(message);
                message.marquerPublie();
            } catch (Exception ex) {
                // On laisse le message non publié : il sera réessayé au prochain passage.
                log.error("Échec de publication Outbox id={} topic={} : nouvelle tentative ultérieure",
                        message.getId(), message.getTopic(), ex);
            }
        }
    }

    /** Construit l'enveloppe + les en-têtes et envoie le message sur son topic. */
    private void publier(OutboxMessage message) throws Exception {
        // Le payload est stocké en JSON : on le réhydrate en arbre pour le réemballer dans l'enveloppe.
        JsonNode etat = objectMapper.readTree(message.getPayload());
        DomainEvent<JsonNode> evenement = DomainEvent.creer(
                message.getEventType(), message.getTraceId(), etat);

        // Clé de partition = identifiant de l'agrégat (garantit l'ordre par agrégat).
        String cle = message.getAggregateId().toString();
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(message.getTopic(), cle, evenement);

        Headers entetes = record.headers();
        ajouter(entetes, "eventId", evenement.eventId().toString());
        ajouter(entetes, "eventType", message.getEventType());
        ajouter(entetes, "eventVersion", VERSION_EVENT);
        ajouter(entetes, "aggregateType", message.getAggregateType());
        ajouter(entetes, "aggregateId", cle);
        ajouter(entetes, "occurredAt", Instant.now().toString());
        if (message.getTraceId() != null) {
            ajouter(entetes, "traceId", message.getTraceId());
        }
        ajouter(entetes, "producer", PRODUCTEUR);

        kafkaTemplate.send(record);
        log.debug("Événement relayé topic={} type={} aggregateId={}",
                message.getTopic(), message.getEventType(), cle);
    }

    private void ajouter(Headers entetes, String cle, String valeur) {
        entetes.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
