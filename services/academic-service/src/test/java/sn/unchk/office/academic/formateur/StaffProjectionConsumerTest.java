package sn.unchk.office.academic.formateur;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.academic.config.ProcessedEvent;
import sn.unchk.office.academic.config.ProcessedEventRepository;
import sn.unchk.office.academic.formateur.event.StaffPayload;
import sn.unchk.office.common.messaging.DomainEvent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du consommateur people.staff alimentant la projection locale des formateurs.
 * On vérifie l'upsert, l'idempotence (rejeu) et la suppression — le tout SANS appel REST.
 */
@ExtendWith(MockitoExtension.class)
class StaffProjectionConsumerTest {

    @Mock
    private FormateurRoRepository formateurRoRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private StaffProjectionConsumer consumer;

    @BeforeEach
    void preparer() {
        consumer = new StaffProjectionConsumer(formateurRoRepository, processedEventRepository, objectMapper);
    }

    @Test
    void consomme_un_staff_cree_la_projection() {
        // Étant donné un événement people.staff (Created) pour un nouveau formateur...
        UUID staffId = UUID.randomUUID();
        StaffPayload payload = new StaffPayload(
                staffId, "Awa", "Diop", "enseignant", "Mathématiques", true, null);
        DomainEvent<Object> evenement = new DomainEvent<>(
                UUID.randomUUID(), "Created", Instant.now(), "trace-1", payload);
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(formateurRoRepository.findById(staffId)).thenReturn(Optional.empty());

        // Quand on consomme l'événement à l'offset 5...
        consumer.consommer(evenement, 5L);

        // Alors une entrée de projection est créée avec le nom complet et l'offset.
        ArgumentCaptor<FormateurRo> capteur = ArgumentCaptor.forClass(FormateurRo.class);
        verify(formateurRoRepository).save(capteur.capture());
        FormateurRo projete = capteur.getValue();
        assertThat(projete.getId()).isEqualTo(staffId);
        assertThat(projete.getFullName()).isEqualTo("Awa Diop");
        assertThat(projete.getKind()).isEqualTo("enseignant");
        assertThat(projete.getSpeciality()).isEqualTo("Mathématiques");
        assertThat(projete.isActive()).isTrue();
        assertThat(projete.getEventOffset()).isEqualTo(5L);
        // L'événement est marqué traité (idempotence).
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void ignore_un_evenement_deja_traite() {
        // Étant donné un événement dont l'eventId est déjà connu...
        UUID eventId = UUID.randomUUID();
        StaffPayload payload = new StaffPayload(
                UUID.randomUUID(), "Moussa", "Sow", "tuteur", null, true, null);
        DomainEvent<Object> evenement = new DomainEvent<>(eventId, "Updated", Instant.now(), null, payload);
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        // Quand on le consomme, alors aucune écriture n'est effectuée (idempotence).
        consumer.consommer(evenement, 1L);

        verify(formateurRoRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void suppression_retire_la_projection() {
        // Étant donné un événement de suppression (Deleted) d'un formateur...
        UUID staffId = UUID.randomUUID();
        StaffPayload payload = new StaffPayload(staffId, "X", "Y", "enseignant", null, false, Instant.now());
        DomainEvent<Object> evenement = new DomainEvent<>(
                UUID.randomUUID(), "Deleted", Instant.now(), null, payload);
        when(processedEventRepository.existsById(any())).thenReturn(false);

        // Quand on le consomme...
        consumer.consommer(evenement, 9L);

        // Alors l'entrée de projection est retirée et aucune mise à jour n'est faite.
        verify(formateurRoRepository).deleteById(staffId);
        verify(formateurRoRepository, never()).save(any());
    }
}
