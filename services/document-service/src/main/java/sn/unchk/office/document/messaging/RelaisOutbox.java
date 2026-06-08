package sn.unchk.office.document.messaging;

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
import sn.unchk.office.document.domain.OutboxMessage;
import sn.unchk.office.document.repository.OutboxRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Relais Outbox → Kafka.
 * <p>
 * Lit périodiquement les messages d'outbox non encore publiés et les émet sur leur topic
 * (ici {@code document.documents}). Ce découplage garantit l'atomicité « base + Kafka » :
 * l'écriture métier et l'écriture d'outbox sont dans la même transaction ; la publication
 * réelle est différée et idempotente (réémission tant que {@code publishedAt} est null).
 * <p>
 * La valeur du message est l'enveloppe {@link DomainEvent} (payload JSON) ; les métadonnées
 * d'enveloppe (eventId, eventType, ...) sont aussi posées dans les en-têtes Kafka.
 */
@Component
public class RelaisOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelaisOutbox.class);

    /** Taille du lot publié à chaque passage du relais. */
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
     * Publie les messages en attente. Exécuté à intervalle régulier.
     */
    @Scheduled(fixedDelayString = "${document.outbox.intervalle-ms:2000}")
    @Transactional
    public void publierEnAttente() {
        List<OutboxMessage> lot = outboxRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, TAILLE_LOT));
        if (lot.isEmpty()) {
            return;
        }
        for (OutboxMessage message : lot) {
            try {
                publier(message);
                message.marquerPublie();
            } catch (Exception ex) {
                // On laisse le message en attente : il sera retenté au prochain passage.
                log.error("Publication Outbox échouée pour le message {} : sera retenté",
                        message.getId(), ex);
            }
        }
    }

    /** Construit l'enregistrement Kafka (valeur = DomainEvent, en-têtes = enveloppe) et l'émet. */
    private void publier(OutboxMessage message) throws Exception {
        // On désérialise le payload JSON stocké pour le replacer dans l'enveloppe DomainEvent.
        Object payload = objectMapper.readTree(message.getPayload());
        // L'eventId est STABLE (= id de l'outbox) : une réémission garde le même identifiant,
        // ce qui préserve l'idempotence côté consommateur (déduplication sur eventId).
        DomainEvent<Object> evenement = new DomainEvent<>(
                message.getId(),
                message.getEventType(),
                Instant.now(),
                message.getTraceId(),
                payload);

        ProducerRecord<String, Object> enregistrement = new ProducerRecord<>(
                message.getTopic(),
                message.getAggregateId().toString(),
                evenement);

        Headers headers = enregistrement.headers();
        ajouter(headers, EnteteEvenement.EVENT_ID, evenement.eventId().toString());
        ajouter(headers, EnteteEvenement.EVENT_TYPE, message.getEventType());
        ajouter(headers, EnteteEvenement.EVENT_VERSION, String.valueOf(message.getEventVersion()));
        ajouter(headers, EnteteEvenement.AGGREGATE_TYPE, message.getAggregateType());
        ajouter(headers, EnteteEvenement.AGGREGATE_ID, message.getAggregateId().toString());
        ajouter(headers, EnteteEvenement.OCCURRED_AT, evenement.occurredAt().toString());
        ajouter(headers, EnteteEvenement.PRODUCER, EnteteEvenement.NOM_PRODUCTEUR);
        if (message.getTraceId() != null) {
            ajouter(headers, EnteteEvenement.TRACE_ID, message.getTraceId());
        }

        // get() : on attend la confirmation pour ne marquer publié qu'en cas de succès.
        kafkaTemplate.send(enregistrement).get();
        log.debug("Événement publié topic={} type={} aggregateId={}",
                message.getTopic(), message.getEventType(), message.getAggregateId());
    }

    private void ajouter(Headers headers, String cle, String valeur) {
        headers.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
