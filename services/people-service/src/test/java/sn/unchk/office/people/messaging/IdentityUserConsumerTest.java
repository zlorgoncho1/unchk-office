package sn.unchk.office.people.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.people.domain.IdentityUserRo;
import sn.unchk.office.people.domain.ProcessedEvent;
import sn.unchk.office.people.messaging.consumer.IdentityUserConsumer;
import sn.unchk.office.people.repository.IdentityUserRoRepository;
import sn.unchk.office.people.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du consommateur du topic identity.users (read-model local).
 * <p>
 * On verifie l'upsert de la projection a partir du payload JSON, et surtout
 * l'idempotence : un evenement deja traite (meme eventId) est ignore et ne
 * reprojette rien (aucun appel REST n'est jamais effectue).
 */
@ExtendWith(MockitoExtension.class)
class IdentityUserConsumerTest {

    @Mock
    private IdentityUserRoRepository userRoRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Test
    @DisplayName("Un evenement identity.users alimente le read-model et marque l'event traite")
    void consommer_upsertReadModel() {
        IdentityUserConsumer consumer =
                new IdentityUserConsumer(userRoRepository, processedEventRepository);

        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "id", userId.toString(),
                "fullName", "Awa Diop",
                "email", "awa.diop@unchk.sn",
                "roles", List.of("etudiant"),
                "isActive", true);
        DomainEvent<Map<String, Object>> evenement =
                new DomainEvent<>(eventId, "Created", Instant.now(), "trace-1", payload);
        ConsumerRecord<String, DomainEvent<Map<String, Object>>> record =
                new ConsumerRecord<>(Topics.IDENTITY_USERS, 0, 5L, userId.toString(), evenement);

        // Event jamais vu, et aucune projection existante : on en cree une.
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(userRoRepository.findById(userId)).thenReturn(Optional.empty());

        consumer.consommer(record);

        // La projection est enregistree avec les champs du payload.
        ArgumentCaptor<IdentityUserRo> capteur = ArgumentCaptor.forClass(IdentityUserRo.class);
        verify(userRoRepository).save(capteur.capture());
        IdentityUserRo vue = capteur.getValue();
        assertThat(vue.getId()).isEqualTo(userId);
        assertThat(vue.getFullName()).isEqualTo("Awa Diop");
        assertThat(vue.getRoles()).containsExactly("etudiant");
        assertThat(vue.getEventOffset()).isEqualTo(5L);

        // L'event est marque comme traite (idempotence).
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Un evenement deja traite est ignore (idempotence)")
    void consommer_evenementDejaTraite_ignore() {
        IdentityUserConsumer consumer =
                new IdentityUserConsumer(userRoRepository, processedEventRepository);

        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        DomainEvent<Map<String, Object>> evenement =
                new DomainEvent<>(eventId, "Updated", Instant.now(), "trace-2",
                        Map.of("id", userId.toString(), "fullName", "Awa Diop"));
        ConsumerRecord<String, DomainEvent<Map<String, Object>>> record =
                new ConsumerRecord<>(Topics.IDENTITY_USERS, 0, 6L, userId.toString(), evenement);

        // L'event a deja ete traite : on doit court-circuiter le traitement.
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        consumer.consommer(record);

        // Aucun upsert, aucune re-marque : le doublon est totalement ignore.
        verify(userRoRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un tombstone Kafka (valeur null) purge la cle du read-model")
    void consommer_tombstone_purge() {
        IdentityUserConsumer consumer =
                new IdentityUserConsumer(userRoRepository, processedEventRepository);

        UUID userId = UUID.randomUUID();
        ConsumerRecord<String, DomainEvent<Map<String, Object>>> record =
                new ConsumerRecord<>(Topics.IDENTITY_USERS, 0, 7L, userId.toString(), null);

        consumer.consommer(record);

        // La cle est retiree de la projection ; aucun upsert.
        verify(userRoRepository, times(1)).deleteById(userId);
        verify(userRoRepository, never()).save(any());
    }
}
