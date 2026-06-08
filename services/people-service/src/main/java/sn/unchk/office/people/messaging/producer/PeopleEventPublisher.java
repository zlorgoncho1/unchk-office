package sn.unchk.office.people.messaging.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.people.domain.Staff;
import sn.unchk.office.people.domain.Student;
import sn.unchk.office.people.messaging.event.StaffPayload;
import sn.unchk.office.people.messaging.event.StudentPayload;
import sn.unchk.office.people.messaging.event.TombstonePayload;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Producteur Kafka des evenements canoniques people.
 * <p>
 * Emet sur {@code people.students} et {@code people.staff} a chaque changement
 * (creation, modification, suppression). La valeur du message porte l'enveloppe
 * {@link DomainEvent} (etat de l'agregat) ; l'enveloppe technique complementaire
 * vit dans les en-tetes Kafka, conformement a docs/architecture.md :
 * {@code eventId, eventType, eventVersion, aggregateType, aggregateId, occurredAt,
 * traceId, producer}.
 * <p>
 * La cle de partition est l'UUID de l'agregat (ordre garanti par cle).
 */
@Component
public class PeopleEventPublisher {

    /** Nom du service producteur, recopie dans l'en-tete {@code producer}. */
    private static final String PRODUCTEUR = "people-service";

    /** Version du schema des payloads (evolutions additives uniquement). */
    private static final String VERSION_SCHEMA = "1";

    private static final String AGG_STUDENT = "Student";
    private static final String AGG_STAFF = "Staff";

    /** Cle MDC du correlation-id, posee par le filtre commun. */
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PeopleEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publie la creation d'un etudiant. */
    public void publierEtudiantCree(Student etudiant) {
        publierEtudiant(etudiant, "Created");
    }

    /** Publie la modification d'un etudiant. */
    public void publierEtudiantModifie(Student etudiant) {
        publierEtudiant(etudiant, "Updated");
    }

    /** Publie la suppression logique d'un etudiant (tombstone). */
    public void publierEtudiantSupprime(UUID studentId, UUID supprimePar) {
        TombstonePayload payload = new TombstonePayload(studentId, Instant.now(), supprimePar);
        publier(Topics.PEOPLE_STUDENTS, studentId, AGG_STUDENT, "Deleted", payload);
    }

    /** Publie la creation d'un personnel / formateur. */
    public void publierPersonnelCree(Staff staff) {
        publierStaff(staff, "Created");
    }

    /** Publie la modification d'un personnel / formateur. */
    public void publierPersonnelModifie(Staff staff) {
        publierStaff(staff, "Updated");
    }

    /** Publie la suppression logique d'un personnel (tombstone). */
    public void publierPersonnelSupprime(UUID staffId, UUID supprimePar) {
        TombstonePayload payload = new TombstonePayload(staffId, Instant.now(), supprimePar);
        publier(Topics.PEOPLE_STAFF, staffId, AGG_STAFF, "Deleted", payload);
    }

    private void publierEtudiant(Student etudiant, String typeEvenement) {
        publier(Topics.PEOPLE_STUDENTS, etudiant.getId(), AGG_STUDENT, typeEvenement,
                StudentPayload.depuis(etudiant));
    }

    private void publierStaff(Staff staff, String typeEvenement) {
        publier(Topics.PEOPLE_STAFF, staff.getId(), AGG_STAFF, typeEvenement,
                StaffPayload.depuis(staff));
    }

    /**
     * Construit l'enveloppe et le record Kafka (en-tetes + cle de partition), puis publie.
     */
    private <T> void publier(String topic, UUID aggregateId, String aggregateType,
                             String typeEvenement, T payload) {
        String traceId = MDC.get(CLE_CORRELATION);
        DomainEvent<T> evenement = DomainEvent.creer(typeEvenement, traceId, payload);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, aggregateId.toString(), evenement);

        Headers entetes = record.headers();
        ajouterEntete(entetes, "eventId", evenement.eventId().toString());
        ajouterEntete(entetes, "eventType", typeEvenement);
        ajouterEntete(entetes, "eventVersion", VERSION_SCHEMA);
        ajouterEntete(entetes, "aggregateType", aggregateType);
        ajouterEntete(entetes, "aggregateId", aggregateId.toString());
        ajouterEntete(entetes, "occurredAt", evenement.occurredAt().toString());
        if (traceId != null) {
            ajouterEntete(entetes, "traceId", traceId);
        }
        ajouterEntete(entetes, "producer", PRODUCTEUR);

        kafkaTemplate.send(record);
    }

    private void ajouterEntete(Headers entetes, String cle, String valeur) {
        entetes.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
