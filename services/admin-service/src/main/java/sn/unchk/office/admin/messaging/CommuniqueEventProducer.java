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
 * Producteur Kafka des communiqués administratifs sur le topic {@code admin.communiques}.
 * <p>
 * Émis uniquement à la publication d'un communiqué : le communication-service consomme cet
 * événement et notifie les rôles destinataires (« notification automatique à chaque nouvelle
 * note de service / circulaire »). Communication 100% Kafka : aucun appel REST inter-service.
 */
@Component
public class CommuniqueEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CommuniqueEventProducer.class);

    private static final String NOM_PRODUCTEUR = "admin-service";
    private static final String TYPE_AGREGAT = "AdminCommunique";
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CommuniqueEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publie l'événement de publication d'un communiqué.
     *
     * @param eventType type d'événement métier (ex : "CommuniquePublie")
     * @param payload   état utile à la notification (id, nature, titre, rôles cibles)
     */
    public void publier(String eventType, CommuniqueEventPayload payload) {
        String traceId = MDC.get(CLE_CORRELATION);
        DomainEvent<CommuniqueEventPayload> evenement = DomainEvent.creer(eventType, traceId, payload);

        String cle = payload.id().toString();
        ProducerRecord<String, Object> enregistrement =
                new ProducerRecord<>(Topics.ADMIN_COMMUNIQUES, cle, evenement);

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
        log.info("Événement communiqué publié type={} communiqueId={}", eventType, cle);
    }

    private void ajouterEntete(Headers entetes, String cle, String valeur) {
        entetes.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
