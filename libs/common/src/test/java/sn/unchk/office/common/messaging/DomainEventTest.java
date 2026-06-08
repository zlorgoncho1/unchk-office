package sn.unchk.office.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de l'enveloppe d'événement de domaine {@link DomainEvent}.
 * Vérifie la fabrique et la sérialisation/désérialisation JSON (aller-retour).
 */
class DomainEventTest {

    /** Charge utile métier minimale servant aux tests. */
    record Etudiant(UUID id, String nom) {
    }

    /** Mapper JSON configuré comme côté Kafka (support des types date/heure Java 8+). */
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void la_fabrique_genere_identifiant_et_horodatage() {
        // Quand on crée un événement via la fabrique
        DomainEvent<Etudiant> evt = DomainEvent.creer(
                "EtudiantCree", "trace-123", new Etudiant(UUID.randomUUID(), "Diop"));

        // Alors l'identifiant et l'horodatage sont renseignés automatiquement
        assertThat(evt.eventId()).isNotNull();
        assertThat(evt.occurredAt()).isNotNull();
        assertThat(evt.eventType()).isEqualTo("EtudiantCree");
        assertThat(evt.traceId()).isEqualTo("trace-123");
        assertThat(evt.payload().nom()).isEqualTo("Diop");
    }

    @Test
    void la_serialisation_json_conserve_tous_les_champs() throws Exception {
        // Étant donné un événement complet
        UUID idEtudiant = UUID.randomUUID();
        DomainEvent<Etudiant> origine = new DomainEvent<>(
                UUID.randomUUID(),
                "EtudiantCree",
                Instant.parse("2026-06-08T10:15:30Z"),
                "trace-abc",
                new Etudiant(idEtudiant, "Ndiaye"));

        // Quand on sérialise puis désérialise (aller-retour JSON)
        String json = mapper.writeValueAsString(origine);
        DomainEvent<?> relu = mapper.readValue(json, DomainEvent.class);

        // Alors les champs de l'enveloppe sont préservés
        assertThat(json).contains("\"eventType\":\"EtudiantCree\"");
        assertThat(json).contains("\"traceId\":\"trace-abc\"");
        assertThat(relu.eventId()).isEqualTo(origine.eventId());
        assertThat(relu.eventType()).isEqualTo("EtudiantCree");
        assertThat(relu.occurredAt()).isEqualTo(origine.occurredAt());
        assertThat(relu.traceId()).isEqualTo("trace-abc");
    }
}
