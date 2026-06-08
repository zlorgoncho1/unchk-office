package sn.unchk.office.communication.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.messaging.consumer.payload.UtilisateurProjete;
import sn.unchk.office.communication.projection.IdentityUserRo;
import sn.unchk.office.communication.repository.IdentityUserRoRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consommateur du topic {@code identity.users} : alimente le read-model {@code identity_user_ro}.
 * <p>
 * Sert à résoudre les destinataires des notifications par rôle, sans aucun appel REST vers
 * identity-service (CQRS). Idempotent (déduplication sur {@code eventId}).
 */
@Component
public class ConsommateurIdentity {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurIdentity.class);

    private final IdentityUserRoRepository utilisateursRo;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurIdentity(IdentityUserRoRepository utilisateursRo,
                                ServiceIdempotence idempotence,
                                ObjectMapper objectMapper) {
        this.utilisateursRo = utilisateursRo;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "identity.users", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return; // déjà traité
        }
        String type = LecteurEnveloppe.eventType(record.headers(), enveloppe);

        UtilisateurProjete projete =
                LecteurEnveloppe.payload(enveloppe, UtilisateurProjete.class, objectMapper);
        if (projete == null || projete.id() == null) {
            log.warn("Événement identity.users sans charge utile exploitable, ignoré");
            return;
        }

        if (estSuppression(type)) {
            utilisateursRo.deleteById(projete.id());
            log.debug("Read-model identity_user_ro supprimé id={}", projete.id());
            return;
        }

        IdentityUserRo ro = utilisateursRo.findById(projete.id())
                .orElseGet(() -> new IdentityUserRo(projete.id()));
        if (projete.fullName() != null) {
            ro.setFullName(projete.fullName());
        }
        ro.setEmail(projete.email());
        List<String> roles = projete.roles();
        ro.setRoles(roles != null ? roles.toArray(new String[0]) : new String[0]);
        ro.setActive(projete.active() == null || projete.active());
        ro.setLastEventAt(Instant.now());
        ro.setEventOffset(record.offset());
        utilisateursRo.save(ro);
        log.debug("Read-model identity_user_ro mis à jour id={}", projete.id());
    }

    /** Détecte un événement de suppression (tombstone logique). */
    private boolean estSuppression(String type) {
        return type != null && type.toLowerCase().contains("delet");
    }
}
