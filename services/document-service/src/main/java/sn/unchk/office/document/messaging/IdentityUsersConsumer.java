package sn.unchk.office.document.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.document.service.ProjectionUtilisateurService;

import java.util.Map;

/**
 * Consommateur du topic {@code identity.users} : maintient le read-model local des comptes.
 * <p>
 * Projection CQRS : le document-service ne fait JAMAIS de REST vers identity-service. Il
 * reconstruit sa copie en lecture seule (rôles, statut) à partir de Kafka. Idempotence
 * assurée en amont via la table {@code processed_events} (clé eventId).
 */
@Component
public class IdentityUsersConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdentityUsersConsumer.class);

    private final ProjectionUtilisateurService projection;

    public IdentityUsersConsumer(ProjectionUtilisateurService projection) {
        this.projection = projection;
    }

    /**
     * Reçoit un événement de cycle de vie d'un compte et met à jour la projection locale.
     *
     * @param evenement enveloppe DomainEvent (payload = état du compte)
     * @param cle       clé de partition (userId)
     */
    @KafkaListener(
            topics = Topics.IDENTITY_USERS,
            groupId = "document-service-identity",
            containerFactory = "kafkaListenerContainerFactory")
    public void surEvenement(@Payload(required = false) DomainEvent<Map<String, Object>> evenement,
                             @Header(name = "kafka_receivedMessageKey", required = false) String cle) {
        if (evenement == null) {
            // Tombstone de compaction (valeur null) : rien à projeter.
            log.debug("Tombstone reçu sur identity.users pour la clé {}", cle);
            return;
        }
        try {
            projection.appliquer(evenement);
        } catch (Exception ex) {
            // On journalise ; la gestion d'erreur fine (retry/DLT) est portée par la config commune.
            log.error("Échec de projection identity.users eventId={}", evenement.eventId(), ex);
            throw ex;
        }
    }
}
