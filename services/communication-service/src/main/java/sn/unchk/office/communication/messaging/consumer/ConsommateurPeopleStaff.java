package sn.unchk.office.communication.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.messaging.consumer.payload.StaffProjete;
import sn.unchk.office.communication.projection.PeopleStaffRo;
import sn.unchk.office.communication.repository.PeopleStaffRoRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Consommateur du topic {@code people.staff} : alimente le read-model {@code people_staff_ro}.
 * <p>
 * Permet d'afficher le nom de l'auteur d'un compte rendu ou de l'organisateur d'une réunion
 * sans appel REST vers people-service. Idempotent.
 */
@Component
public class ConsommateurPeopleStaff {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurPeopleStaff.class);

    private final PeopleStaffRoRepository staffRo;
    private final ServiceIdempotence idempotence;
    private final ObjectMapper objectMapper;

    public ConsommateurPeopleStaff(PeopleStaffRoRepository staffRo,
                                   ServiceIdempotence idempotence,
                                   ObjectMapper objectMapper) {
        this.staffRo = staffRo;
        this.idempotence = idempotence;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "people.staff", groupId = "communication-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<Object> enveloppe = record.value();
        UUID eventId = LecteurEnveloppe.eventId(record.headers(), enveloppe);
        if (!idempotence.marquerSiNouveau(eventId)) {
            return;
        }
        String type = LecteurEnveloppe.eventType(record.headers(), enveloppe);

        StaffProjete projete = LecteurEnveloppe.payload(enveloppe, StaffProjete.class, objectMapper);
        if (projete == null || projete.id() == null) {
            log.warn("Événement people.staff sans charge utile exploitable, ignoré");
            return;
        }

        if (type != null && type.toLowerCase().contains("delet")) {
            staffRo.deleteById(projete.id());
            log.debug("Read-model people_staff_ro supprimé id={}", projete.id());
            return;
        }

        PeopleStaffRo ro = staffRo.findById(projete.id())
                .orElseGet(() -> new PeopleStaffRo(projete.id()));
        if (projete.fullName() != null) {
            ro.setFullName(projete.fullName());
        }
        ro.setKind(projete.kind() != null ? projete.kind() : "inconnu");
        ro.setLastEventAt(Instant.now());
        ro.setEventOffset(record.offset());
        staffRo.save(ro);
        log.debug("Read-model people_staff_ro mis à jour id={}", projete.id());
    }
}
