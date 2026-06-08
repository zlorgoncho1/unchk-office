package sn.unchk.office.communication.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.domain.NotificationKind;
import sn.unchk.office.communication.messaging.payload.ReunionEvent;
import sn.unchk.office.communication.service.ServiceNotification;

import java.util.UUID;

/**
 * Consommateur du topic {@code communication.reunions} (le service consomme son propre topic).
 * <p>
 * À la planification d'une réunion ({@code ReunionPlanifiee}), envoie une convocation à chaque
 * participant (destinataires explicites issus de l'événement). Aucun appel REST.
 */
@Component
public class ConsommateurReunion {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurReunion.class);

    /** Type d'événement déclenchant les convocations. */
    private static final String PLANIFIEE = "ReunionPlanifiee";

    private final ServiceNotification serviceNotification;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurReunion(ServiceNotification serviceNotification,
                               ServiceIdempotence idempotence,
                               ObjectMapper objectMapper) {
        this.serviceNotification = serviceNotification;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "communication.reunions", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }
        String type = LecteurEnveloppe.eventType(record.headers(), enveloppe);
        if (!PLANIFIEE.equals(type)) {
            return;
        }

        ReunionEvent event = LecteurEnveloppe.payload(enveloppe, ReunionEvent.class, objectMapper);
        if (event == null || event.id() == null) {
            log.warn("Événement {} sans charge utile exploitable, ignoré", PLANIFIEE);
            return;
        }
        if (event.participantIds() == null || event.participantIds().isEmpty()) {
            return;
        }

        String titre = "Convocation : " + event.title();
        String message = "Vous êtes invité(e) à une réunion.";
        serviceNotification.notifierDestinataires(
                event.participantIds(),
                NotificationKind.reunion.name(),
                titre,
                message,
                "communication",
                event.id());
        log.debug("Convocations déclenchées pour la réunion id={} ({} participant(s))",
                event.id(), event.participantIds().size());
    }
}
