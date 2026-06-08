package sn.unchk.office.common.authz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du mapping de l'entrée OPA {@link EntreeOpa}.
 * Vérifie que la structure JSON produite correspond EXACTEMENT à ce qu'attend
 * la politique Rego {@code unchk.authz} (subject / action / resource / request).
 */
class EntreeOpaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void l_enveloppe_respecte_le_format_attendu_par_la_politique_rego() throws Exception {
        // Étant donné une entrée complète (lecture d'un document par un enseignant)
        EntreeOpa entree = new EntreeOpa(
                new EntreeOpa.Sujet("u-123", List.of("enseignant")),
                "read",
                new EntreeOpa.Ressource("document", "d-1", "u-9", List.of("enseignant", "admin")),
                new EntreeOpa.Requete("GET", "/api/documents/d-1"));

        // Quand on construit l'enveloppe et qu'on la sérialise
        Map<String, Object> enveloppe = entree.versEnveloppe();
        String json = mapper.writeValueAsString(enveloppe);
        JsonNode racine = mapper.readTree(json);

        // Alors l'enveloppe contient bien la clé "input"
        JsonNode input = racine.get("input");
        assertThat(input).isNotNull();

        // Et le sujet expose id + roles
        assertThat(input.get("subject").get("id").asText()).isEqualTo("u-123");
        assertThat(input.get("subject").get("roles").get(0).asText()).isEqualTo("enseignant");

        // Et l'action est correcte
        assertThat(input.get("action").asText()).isEqualTo("read");

        // Et la ressource expose type, id, ownerId et visibility
        JsonNode ressource = input.get("resource");
        assertThat(ressource.get("type").asText()).isEqualTo("document");
        assertThat(ressource.get("id").asText()).isEqualTo("d-1");
        assertThat(ressource.get("ownerId").asText()).isEqualTo("u-9");
        assertThat(ressource.get("visibility").get(1).asText()).isEqualTo("admin");

        // Et la requête expose method + path (pour le RBAC de route)
        assertThat(input.get("request").get("method").asText()).isEqualTo("GET");
        assertThat(input.get("request").get("path").asText()).isEqualTo("/api/documents/d-1");
    }
}
