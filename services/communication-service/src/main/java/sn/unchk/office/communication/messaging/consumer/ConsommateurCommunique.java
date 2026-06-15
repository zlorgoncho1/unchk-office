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
import sn.unchk.office.communication.messaging.consumer.payload.CommuniqueProjete;
import sn.unchk.office.communication.service.ServiceNotification;

import java.util.UUID;

/**
 * Consommateur du topic {@code admin.communiques}.
 * <p>
 * À la publication d'une note de service ou d'une circulaire (admin-service), notifie tous les
 * utilisateurs dont un rôle figure dans le ciblage du communiqué. C'est le pendant « note de
 * service / circulaire émise par l'Administration » de la règle « notification automatique à
 * chaque nouveau compte rendu / circulaire ». Résolution des destinataires sur le read-model
 * local {@code identity_user_ro} (zéro appel REST). Idempotence sur {@code eventId}.
 */
@Component
public class ConsommateurCommunique {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurCommunique.class);

    /** Nature « circulaire » (les autres natures sont traitées en note de service). */
    private static final String CIRCULAIRE = "circulaire";

    private final ServiceNotification serviceNotification;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurCommunique(ServiceNotification serviceNotification,
                                  ServiceIdempotence idempotence,
                                  ObjectMapper objectMapper) {
        this.serviceNotification = serviceNotification;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "admin.communiques", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }

        CommuniqueProjete c = LecteurEnveloppe.payload(enveloppe, CommuniqueProjete.class, objectMapper);
        if (c == null || c.id() == null) {
            return;
        }
        // On ne notifie qu'à la publication.
        if (c.published() == null || !c.published()) {
            return;
        }

        boolean circulaire = CIRCULAIRE.equalsIgnoreCase(c.kind());
        String kind = circulaire ? NotificationKind.circulaire.name() : NotificationKind.note_service.name();
        String prefixe = circulaire ? "Nouvelle circulaire : " : "Nouvelle note de service : ";
        String titre = prefixe + (c.title() != null ? c.title() : "");
        String message = circulaire
                ? "Une circulaire a été publiée par l'Administration."
                : "Une note de service a été publiée par l'Administration.";

        serviceNotification.notifierParRoles(
                c.targets(), kind, titre, message, "admin", c.id());
        log.debug("Notifications déclenchées pour le communiqué id={} (kind={})", c.id(), c.kind());
    }
}
