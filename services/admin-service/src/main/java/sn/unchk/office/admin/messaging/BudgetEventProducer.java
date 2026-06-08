package sn.unchk.office.admin.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Producteur Kafka des événements budgétaires sur le topic {@code admin.budget}.
 * <p>
 * Émet une enveloppe {@link DomainEvent} (valeur JSON) et renseigne les en-têtes décrits dans
 * docs/architecture.md (eventId, eventType, aggregateType, aggregateId, occurredAt, traceId,
 * producer). La clé de partition est l'UUID du budget pour garantir l'ordre par agrégat.
 * <p>
 * Communication 100% Kafka : ce producteur est le SEUL canal de propagation vers les autres
 * services (academic, communication...), il n'existe aucun appel REST inter-service.
 */
@Component
public class BudgetEventProducer {

    private static final Logger log = LoggerFactory.getLogger(BudgetEventProducer.class);

    /** Nom du service producteur, repris dans l'en-tête {@code producer}. */
    private static final String NOM_PRODUCTEUR = "admin-service";

    /** Type d'agrégat porté par le topic. */
    private static final String TYPE_AGREGAT = "Budget";

    /** Clé MDC de corrélation posée par le filtre web (libs/common). */
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BudgetEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publie l'état d'un budget après une modification métier.
     *
     * @param eventType type d'événement métier ("BudgetCree", "BudgetMisAJour", "BudgetVote"...)
     * @param payload   état de l'agrégat budget (state transfer)
     */
    public void publier(String eventType, BudgetEventPayload payload) {
        String traceId = MDC.get(CLE_CORRELATION);
        DomainEvent<BudgetEventPayload> evenement = DomainEvent.creer(eventType, traceId, payload);

        // Clé = UUID du budget : ordre garanti par agrégat (cf. clé de partition).
        String cle = payload.budgetId().toString();
        ProducerRecord<String, Object> enregistrement =
                new ProducerRecord<>(Topics.ADMIN_BUDGET, cle, evenement);

        // Enveloppe transportée dans les en-têtes Kafka (pas dans le payload).
        Headers entetes = enregistrement.headers();
        ajouterEntete(entetes, "eventId", evenement.eventId().toString());
        ajouterEntete(entetes, "eventType", eventType);
        ajouterEntete(entetes, "eventVersion", "1");
        ajouterEntete(entetes, "aggregateType", TYPE_AGREGAT);
        ajouterEntete(entetes, "aggregateId", cle);
        ajouterEntete(entetes, "occurredAt", evenement.occurredAt() != null
                ? evenement.occurredAt().toString() : Instant.now().toString());
        if (traceId != null) {
            ajouterEntete(entetes, "traceId", traceId);
        }
        ajouterEntete(entetes, "producer", NOM_PRODUCTEUR);

        kafkaTemplate.send(enregistrement);
        log.info("Événement budget publié type={} budgetId={}", eventType, cle);
    }

    /** Ajoute un en-tête Kafka sous forme d'octets UTF-8. */
    private void ajouterEntete(Headers entetes, String cle, String valeur) {
        entetes.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
