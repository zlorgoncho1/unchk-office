package sn.unchk.office.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.messaging.consumer.LecteurEnveloppe;
import sn.unchk.office.communication.messaging.consumer.payload.UtilisateurProjete;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de l'extraction d'enveloppe (en-têtes Kafka + conversion du payload).
 */
class LecteurEnveloppeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void eventId_est_lu_en_priorite_depuis_l_entete() {
        // Étant donné un en-tête eventId et une enveloppe avec un autre eventId
        UUID idEntete = UUID.randomUUID();
        Headers entetes = new RecordHeaders();
        entetes.add("eventId", idEntete.toString().getBytes(StandardCharsets.UTF_8));
        DomainEvent<Object> enveloppe = DomainEvent.creer("Type", null, Map.of());

        // Quand on lit l'eventId / Alors l'en-tête prime
        assertThat(LecteurEnveloppe.eventId(entetes, enveloppe)).isEqualTo(idEntete);
    }

    @Test
    void payload_est_converti_vers_le_type_cible() {
        // Étant donné une enveloppe dont le payload est une map (cas désérialisation générique)
        UUID id = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "id", id.toString(),
                "fullName", "Awa Diop",
                "roles", List.of("enseignant"),
                "active", true);
        DomainEvent<Object> enveloppe = DomainEvent.creer("Updated", null, payload);

        // Quand on convertit / Alors on récupère un UtilisateurProjete exploitable
        UtilisateurProjete projete =
                LecteurEnveloppe.payload(enveloppe, UtilisateurProjete.class, objectMapper);

        assertThat(projete).isNotNull();
        assertThat(projete.id()).isEqualTo(id);
        assertThat(projete.fullName()).isEqualTo("Awa Diop");
        assertThat(projete.roles()).containsExactly("enseignant");
        assertThat(projete.active()).isTrue();
    }
}
