package sn.unchk.office.people.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.Staff;
import sn.unchk.office.people.domain.StaffKind;
import sn.unchk.office.people.messaging.event.StaffPayload;
import sn.unchk.office.people.messaging.producer.PeopleEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests du producteur d'evenements people.
 * <p>
 * On verifie que l'evenement est publie sur le bon topic, avec l'UUID de l'agregat
 * comme cle de partition, et que les en-tetes de l'enveloppe (eventType, aggregateType,
 * producer...) sont correctement positionnes conformement a docs/architecture.md.
 */
@ExtendWith(MockitoExtension.class)
class PeopleEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("La creation d'un personnel publie sur people.staff avec les bons en-tetes")
    void publierPersonnelCree_enTetesCorrects() {
        PeopleEventPublisher publisher = new PeopleEventPublisher(kafkaTemplate);

        Staff staff = new Staff();
        UUID id = UUID.randomUUID();
        staff.setId(id);
        staff.setFirstName("Cheikh");
        staff.setLastName("Ndiaye");
        staff.setGender(Genre.homme);
        staff.setKind(StaffKind.enseignant);
        staff.setActive(true);

        publisher.publierPersonnelCree(staff);

        // On capture le record reellement envoye a Kafka.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, Object>> capteur =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(capteur.capture());
        ProducerRecord<String, Object> record = capteur.getValue();

        // Topic et cle de partition (UUID de l'agregat).
        assertThat(record.topic()).isEqualTo(Topics.PEOPLE_STAFF);
        assertThat(record.key()).isEqualTo(id.toString());

        // La valeur est bien l'enveloppe DomainEvent portant le payload du personnel.
        assertThat(record.value()).isInstanceOf(DomainEvent.class);
        DomainEvent<?> evenement = (DomainEvent<?>) record.value();
        assertThat(evenement.eventType()).isEqualTo("Created");
        assertThat(evenement.payload()).isInstanceOf(StaffPayload.class);

        // Les en-tetes techniques sont presents et coherents.
        assertThat(lireEntete(record, "eventType")).isEqualTo("Created");
        assertThat(lireEntete(record, "aggregateType")).isEqualTo("Staff");
        assertThat(lireEntete(record, "aggregateId")).isEqualTo(id.toString());
        assertThat(lireEntete(record, "eventVersion")).isEqualTo("1");
        assertThat(lireEntete(record, "producer")).isEqualTo("people-service");
        assertThat(lireEntete(record, "eventId")).isNotBlank();
        assertThat(lireEntete(record, "occurredAt")).isNotBlank();
    }

    /** Lit la valeur texte d'un en-tete Kafka du record. */
    private String lireEntete(ProducerRecord<String, Object> record, String cle) {
        var entete = record.headers().lastHeader(cle);
        return entete != null ? new String(entete.value(), StandardCharsets.UTF_8) : null;
    }
}
