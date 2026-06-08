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
import sn.unchk.office.communication.messaging.payload.CompteRenduEvent;
import sn.unchk.office.communication.service.ServiceNotification;

import java.util.UUID;

/**
 * Consommateur du topic {@code communication.comptesrendus} (le service consomme son propre topic).
 * <p>
 * À la publication d'un compte rendu ({@code CompteRenduPublie}), résout les destinataires à
 * partir des read-models locaux (utilisateurs actifs dont un rôle figure dans la visibilité du
 * compte rendu) et met en file une notification par destinataire. Aucun appel REST.
 */
@Component
public class ConsommateurCompteRendu {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurCompteRendu.class);

    /** Type d'événement déclenchant les notifications. */
    private static final String PUBLIE = "CompteRenduPublie";

    private final ServiceNotification serviceNotification;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurCompteRendu(ServiceNotification serviceNotification,
                                   ServiceIdempotence idempotence,
                                   ObjectMapper objectMapper) {
        this.serviceNotification = serviceNotification;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "communication.comptesrendus", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }
        String type = LecteurEnveloppe.eventType(record.headers(), enveloppe);
        // Seule la publication déclenche les notifications.
        if (!PUBLIE.equals(type)) {
            return;
        }

        CompteRenduEvent event =
                LecteurEnveloppe.payload(enveloppe, CompteRenduEvent.class, objectMapper);
        if (event == null || event.id() == null) {
            log.warn("Événement {} sans charge utile exploitable, ignoré", PUBLIE);
            return;
        }

        String titre = "Nouveau compte rendu : " + event.title();
        String message = "Un compte rendu a été publié.";
        serviceNotification.notifierParRoles(
                event.visibility(),
                NotificationKind.compte_rendu.name(),
                titre,
                message,
                "communication",
                event.id());
        log.debug("Notifications déclenchées pour le compte rendu publié id={}", event.id());
    }
}
