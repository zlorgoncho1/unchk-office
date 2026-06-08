package sn.unchk.office.academic.formateur;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.academic.config.ProcessedEvent;
import sn.unchk.office.academic.config.ProcessedEventRepository;
import sn.unchk.office.academic.formateur.event.StaffPayload;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.time.Instant;
import java.util.UUID;

/**
 * Consommateur du topic {@code people.staff} : alimente la projection locale des formateurs
 * ({@code academic_formateur_ro}).
 * <p>
 * C'est le cœur de la règle « zéro REST inter-service » : academic-service ne demande jamais
 * les noms des formateurs à people-service, il les reconstruit localement en consommant Kafka.
 * Le traitement est idempotent (déduplication sur {@code eventId}) car les topics compactés
 * peuvent être rejoués depuis l'offset 0.
 */
@Component
public class StaffProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(StaffProjectionConsumer.class);

    private final FormateurRoRepository formateurRoRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public StaffProjectionConsumer(FormateurRoRepository formateurRoRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   ObjectMapper objectMapper) {
        this.formateurRoRepository = formateurRoRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Applique un événement {@code people.staff} à la projection locale.
     *
     * @param evenement enveloppe DomainEvent (le payload est converti en {@link StaffPayload})
     * @param offset    offset Kafka du message (mémorisé pour l'idempotence / ordonnancement)
     */
    @KafkaListener(topics = Topics.PEOPLE_STAFF, groupId = "academic-service")
    @Transactional
    public void consommer(@Payload DomainEvent<Object> evenement,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        if (evenement == null || evenement.payload() == null) {
            // Tombstone Kafka pur (valeur null) ou message vide : rien à projeter.
            return;
        }

        UUID eventId = evenement.eventId();
        // Idempotence : on ignore un événement déjà appliqué (rejeu, retry...).
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.debug("Événement people.staff {} déjà traité, ignoré.", eventId);
            return;
        }

        StaffPayload staff = objectMapper.convertValue(evenement.payload(), StaffPayload.class);
        if (staff == null || staff.staffId() == null) {
            log.warn("Charge utile people.staff invalide (staffId manquant), événement ignoré.");
            return;
        }

        appliquer(evenement.eventType(), staff, offset);

        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
    }

    /** Met à jour ou retire l'entrée de projection selon le type d'événement. */
    private void appliquer(String eventType, StaffPayload staff, long offset) {
        boolean suppression = "Deleted".equals(eventType) || staff.deletedAt() != null;
        if (suppression) {
            // Suppression logique : on retire le formateur de la projection s'il existe.
            formateurRoRepository.deleteById(staff.staffId());
            log.debug("Formateur {} retiré de la projection (eventType={}).", staff.staffId(), eventType);
            return;
        }

        // Upsert : on récupère l'entrée existante ou on en crée une nouvelle.
        FormateurRo projection = formateurRoRepository.findById(staff.staffId())
                .orElseGet(() -> new FormateurRo(staff.staffId()));
        projection.setFullName(staff.nomComplet());
        projection.setKind(staff.kind() != null ? staff.kind() : "inconnu");
        projection.setSpeciality(staff.speciality());
        projection.setActive(staff.active() == null || staff.active());
        projection.setLastEventAt(Instant.now());
        projection.setEventOffset(offset);
        formateurRoRepository.save(projection);
        log.debug("Projection du formateur {} mise à jour (eventType={}).", staff.staffId(), eventType);
    }
}
