package sn.unchk.office.admin.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.admin.projection.PeopleStaffRo;
import sn.unchk.office.admin.projection.ProcessedEvent;
import sn.unchk.office.admin.repository.PeopleStaffRoRepository;
import sn.unchk.office.admin.repository.ProcessedEventRepository;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.time.Instant;
import java.util.UUID;

/**
 * Consommateur Kafka du topic {@code people.staff} : alimente le read-model local
 * {@code people_staff_ro} (projection CQRS).
 * <p>
 * Aucun appel REST inter-service : la copie lecture-seule du personnel est construite
 * uniquement par consommation Kafka. L'idempotence repose sur la table
 * {@code processed_events} (déduplication sur {@code eventId}) : un message rejoué n'est
 * appliqué qu'une fois. Un événement de suppression purge la projection.
 */
@Component
public class PeopleStaffConsumer {

    private static final Logger log = LoggerFactory.getLogger(PeopleStaffConsumer.class);

    private final PeopleStaffRoRepository staffRoRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public PeopleStaffConsumer(PeopleStaffRoRepository staffRoRepository,
                               ProcessedEventRepository processedEventRepository,
                               ObjectMapper objectMapper) {
        this.staffRoRepository = staffRoRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Traite un événement de personnel et met à jour la projection locale.
     *
     * @param enregistrement message brut (clé = staffId, valeur = enveloppe DomainEvent)
     */
    @KafkaListener(topics = Topics.PEOPLE_STAFF, groupId = "${spring.kafka.consumer.group-id:admin-service}")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> enregistrement) {
        DomainEvent<Object> evenement = enregistrement.value();
        if (evenement == null) {
            // Tombstone Kafka (valeur null) sur topic compacté : purge par la clé.
            supprimerParCle(enregistrement.key());
            return;
        }

        // Idempotence : on ignore un événement déjà appliqué.
        if (evenement.eventId() != null && processedEventRepository.existsById(evenement.eventId())) {
            log.debug("Événement people.staff déjà traité, ignoré eventId={}", evenement.eventId());
            return;
        }

        try {
            PeopleStaffPayload payload =
                    objectMapper.convertValue(evenement.payload(), PeopleStaffPayload.class);
            if (payload == null || payload.id() == null) {
                log.warn("Événement people.staff sans identifiant exploitable, ignoré.");
                return;
            }

            String type = evenement.eventType();
            if (type != null && type.toLowerCase().contains("delet")) {
                // Suppression (eventType de type "Deleted" / "Supprime") : on retire la projection.
                staffRoRepository.deleteById(payload.id());
                log.info("Personnel retiré du read-model local id={}", payload.id());
            } else {
                appliquerUpsert(payload, enregistrement.offset());
            }

            marquerTraite(evenement.eventId());
        } catch (IllegalArgumentException ex) {
            // Charge utile illisible : on trace mais on n'interrompt pas la partition.
            log.error("Impossible de projeter l'événement people.staff (payload invalide)", ex);
        }
    }

    /** Insère ou met à jour l'entrée de read-model pour ce personnel. */
    private void appliquerUpsert(PeopleStaffPayload payload, long offset) {
        PeopleStaffRo entree = staffRoRepository.findById(payload.id())
                .orElseGet(() -> new PeopleStaffRo(payload.id(), payload.fullName(),
                        payload.kind(), payload.department(), Instant.now(), offset));
        entree.setFullName(payload.fullName());
        entree.setKind(payload.kind());
        entree.setDepartment(payload.department());
        entree.setLastEventAt(Instant.now());
        entree.setEventOffset(offset);
        staffRoRepository.save(entree);
        log.info("Read-model personnel mis à jour id={}", payload.id());
    }

    /** Supprime la projection à partir de la clé (cas tombstone). */
    private void supprimerParCle(String cle) {
        if (cle == null) {
            return;
        }
        try {
            staffRoRepository.deleteById(UUID.fromString(cle));
            log.info("Read-model personnel purgé (tombstone) id={}", cle);
        } catch (IllegalArgumentException ex) {
            log.warn("Clé de tombstone invalide, purge ignorée : {}", cle);
        }
    }

    /** Marque l'événement comme traité pour garantir l'idempotence. */
    private void marquerTraite(UUID eventId) {
        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
    }
}
