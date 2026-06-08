package sn.unchk.office.insertion.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

/**
 * Producteur Kafka du service d'insertion.
 * <p>
 * Publie les événements de domaine sur le topic {@code insertion.events}, en enveloppe
 * {@link DomainEvent}. La clé de partition est l'UUID de l'étudiant concerné
 * ({@code studentId}, cf. docs/architecture.md), pour regrouper le devenir d'un même
 * étudiant sur une partition. Le {@code traceId} est repris du MDC (corrélation HTTP).
 * <p>
 * C'est le SEUL canal de sortie vers les autres services : aucun appel REST inter-service.
 */
@Component
public class ProducteurInsertion {

    private static final Logger log = LoggerFactory.getLogger(ProducteurInsertion.class);

    /** Clé MDC de corrélation, alignée avec le filtre web de la librairie commune. */
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProducteurInsertion(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publie un événement d'insertion.
     *
     * @param studentId clé de partition (UUID de l'étudiant, en chaîne) ; à défaut une autre clé stable
     * @param eventType type métier de l'événement (cf. {@link EvenementInsertion})
     * @param payload   charge utile métier (sérialisée en JSON)
     */
    public void publier(String studentId, String eventType, Object payload) {
        String traceId = MDC.get(CLE_CORRELATION);
        DomainEvent<Object> evenement = DomainEvent.creer(eventType, traceId, payload);
        kafkaTemplate.send(Topics.INSERTION_EVENTS, studentId, evenement);
        log.debug("Événement insertion publié : type={} cle={} eventId={}",
                eventType, studentId, evenement.eventId());
    }
}
