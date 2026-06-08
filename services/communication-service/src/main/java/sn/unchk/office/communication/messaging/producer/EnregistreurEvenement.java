package sn.unchk.office.communication.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import sn.unchk.office.communication.domain.OutboxMessage;
import sn.unchk.office.communication.repository.OutboxRepository;
import sn.unchk.office.common.web.CorrelationIdFilter;

import java.util.UUID;

/**
 * Enregistre un événement de domaine dans l'Outbox (Transactional Outbox).
 * <p>
 * Appelé DANS la transaction métier : l'écriture de l'agrégat et la mise en file de
 * l'événement sont atomiques. Le relais {@link RelaisOutbox} publie ensuite sur Kafka.
 * Aucune publication Kafka directe ici : c'est ce qui garantit qu'on ne perd jamais
 * un événement même en cas de panne entre le commit et l'envoi.
 */
@Component
public class EnregistreurEvenement {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public EnregistreurEvenement(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Met en file un événement à publier sur un topic.
     *
     * @param aggregateType type d'agrégat (Reunion, CompteRendu, Notification)
     * @param aggregateId   identifiant de l'agrégat = clé de partition Kafka
     * @param topic         topic de destination
     * @param eventType     type d'événement métier
     * @param payload       charge utile (sera sérialisée en JSON)
     */
    public void enregistrer(String aggregateType, UUID aggregateId, String topic,
                            String eventType, Object payload) {
        String json = serialiser(payload);
        // On propage l'identifiant de corrélation courant pour le tracer jusqu'au consommateur.
        String traceId = MDC.get(CorrelationIdFilter.CLE_MDC);
        OutboxMessage message = new OutboxMessage(
                aggregateType, aggregateId, topic, eventType, traceId, json);
        outboxRepository.save(message);
    }

    /** Sérialise le payload en JSON ; échoue franchement si l'objet n'est pas sérialisable. */
    private String serialiser(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Sérialisation JSON de l'événement impossible", ex);
        }
    }
}
