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
import sn.unchk.office.communication.messaging.consumer.payload.DocumentProjete;
import sn.unchk.office.communication.service.ServiceNotification;

import java.util.UUID;

/**
 * Consommateur du topic {@code document.documents}.
 * <p>
 * À la publication d'un document de catégorie {@code circulaire}, notifie les utilisateurs
 * dont un rôle figure dans la visibilité du document. C'est le pendant « circulaire » de la
 * règle « notification automatique sur nouveau compte rendu / circulaire ». Aucun appel REST :
 * la résolution des destinataires se fait sur le read-model local {@code identity_user_ro}.
 */
@Component
public class ConsommateurDocument {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurDocument.class);

    /** Catégorie de document déclenchant une notification de circulaire. */
    private static final String CIRCULAIRE = "circulaire";

    private final ServiceNotification serviceNotification;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurDocument(ServiceNotification serviceNotification,
                                ServiceIdempotence idempotence,
                                ObjectMapper objectMapper) {
        this.serviceNotification = serviceNotification;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "document.documents", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }

        DocumentProjete doc = LecteurEnveloppe.payload(enveloppe, DocumentProjete.class, objectMapper);
        if (doc == null || doc.id() == null) {
            return;
        }
        // On ne notifie que les circulaires effectivement publiées.
        boolean circulairePubliee = CIRCULAIRE.equalsIgnoreCase(doc.category())
                && Boolean.TRUE.equals(doc.published());
        if (!circulairePubliee) {
            return;
        }

        String titre = "Nouvelle circulaire : " + (doc.title() != null ? doc.title() : "");
        String message = "Une circulaire a été publiée.";
        serviceNotification.notifierParRoles(
                doc.visibility(),
                NotificationKind.circulaire.name(),
                titre,
                message,
                "document",
                doc.id());
        log.debug("Notifications déclenchées pour la circulaire id={}", doc.id());
    }
}
