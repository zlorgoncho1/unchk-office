package sn.unchk.office.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.admin.messaging.PeopleStaffConsumer;
import sn.unchk.office.admin.projection.PeopleStaffRo;
import sn.unchk.office.admin.repository.PeopleStaffRoRepository;
import sn.unchk.office.admin.repository.ProcessedEventRepository;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du consommateur people.staff : projection du read-model local et idempotence.
 */
@ExtendWith(MockitoExtension.class)
class PeopleStaffConsumerTest {

    @Mock
    private PeopleStaffRoRepository staffRoRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PeopleStaffConsumer consumer;

    @Test
    void consommer_upsert_le_read_model_pour_un_nouvel_evenement() {
        // Étant donné un événement de personnel jamais traité
        UUID staffId = UUID.randomUUID();
        DomainEvent<Object> evenement = evenementStaff(staffId, "StaffCree");
        when(processedEventRepository.existsById(evenement.eventId())).thenReturn(false);
        when(staffRoRepository.findById(staffId)).thenReturn(Optional.empty());

        // Quand on le consomme
        consumer.consommer(enregistrement(staffId, evenement));

        // Alors la projection est enregistrée et l'événement marqué comme traité
        verify(staffRoRepository).save(any(PeopleStaffRo.class));
        verify(processedEventRepository).save(any());
    }

    @Test
    void consommer_ignore_un_evenement_deja_traite() {
        // Étant donné un événement dont l'identifiant est déjà connu
        UUID staffId = UUID.randomUUID();
        DomainEvent<Object> evenement = evenementStaff(staffId, "StaffMisAJour");
        when(processedEventRepository.existsById(evenement.eventId())).thenReturn(true);

        // Quand on le consomme
        consumer.consommer(enregistrement(staffId, evenement));

        // Alors aucune écriture n'est faite (idempotence)
        verify(staffRoRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    /** Construit une enveloppe d'événement avec une charge utile de personnel. */
    private DomainEvent<Object> evenementStaff(UUID staffId, String type) {
        Map<String, Object> payload = Map.of(
                "id", staffId.toString(),
                "fullName", "Awa Diop",
                "kind", "administratif",
                "department", "Scolarité");
        return new DomainEvent<>(UUID.randomUUID(), type, Instant.now(), "trace-1", payload);
    }

    /** Construit le ConsumerRecord Kafka correspondant. */
    private ConsumerRecord<String, DomainEvent<Object>> enregistrement(UUID staffId,
                                                                       DomainEvent<Object> evenement) {
        return new ConsumerRecord<>(Topics.PEOPLE_STAFF, 0, 0L, staffId.toString(), evenement);
    }
}
