package sn.unchk.office.communication.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.domain.Notification;
import sn.unchk.office.communication.domain.NotificationKind;
import sn.unchk.office.communication.dto.NotificationDto;
import sn.unchk.office.communication.messaging.payload.NotificationEvent;
import sn.unchk.office.communication.repository.NotificationRepository;
import sn.unchk.office.communication.ws.PousseurNotificationWs;

import java.util.UUID;

/**
 * Consommateur du topic {@code notifications} (le service consomme son propre topic).
 * <p>
 * Persiste la notification (badge + historique) puis tente un push WebSocket vers la session
 * du destinataire. Idempotent : si l'événement a déjà été traité, il n'est pas rejoué.
 */
@Component
public class ConsommateurNotification {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurNotification.class);

    private final NotificationRepository notificationRepository;
    private final PousseurNotificationWs pousseur;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurNotification(NotificationRepository notificationRepository,
                                    PousseurNotificationWs pousseur,
                                    ServiceIdempotence idempotence,
                                    ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.pousseur = pousseur;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "notifications", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }

        NotificationEvent event =
                LecteurEnveloppe.payload(enveloppe, NotificationEvent.class, objectMapper);
        if (event == null || event.recipientId() == null) {
            log.warn("Événement notification sans destinataire, ignoré");
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(event.recipientId());
        notification.setKind(resoudreKind(event.kind()));
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setTargetService(event.targetService());
        notification.setTargetRef(event.targetRef());

        // Push WebSocket : la session est liée au subject.id du destinataire (anti-IDOR).
        boolean pousse = pousseur.pousser(NotificationDto.de(garderId(notification)));
        notification.setDeliveredWs(pousse);

        notificationRepository.save(notification);
        log.debug("Notification persistée destinataire={} (push={})", event.recipientId(), pousse);
    }

    /** Convertit le libellé de catégorie en énumération ; valeur de repli {@code systeme}. */
    private NotificationKind resoudreKind(String kind) {
        if (kind == null) {
            return NotificationKind.systeme;
        }
        try {
            return NotificationKind.valueOf(kind);
        } catch (IllegalArgumentException ex) {
            return NotificationKind.systeme;
        }
    }

    /** Garantit un identifiant pour le DTO poussé (le push précède la persistance). */
    private Notification garderId(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
        }
        return notification;
    }
}
