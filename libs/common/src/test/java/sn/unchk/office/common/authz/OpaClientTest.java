package sn.unchk.office.common.authz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

/**
 * Tests du client OPA {@link OpaClient}.
 * Vérifie l'appel HTTP (URL, méthode, corps "input") et le comportement fail-closed.
 */
class OpaClientTest {

    private RestClient restClient;
    private MockRestServiceServer serveur;
    private OpaClient opaClient;

    /** Entrée d'exemple : lecture d'un document. */
    private EntreeOpa entreeExemple() {
        return new EntreeOpa(
                new EntreeOpa.Sujet("u-123", List.of("enseignant")),
                "read",
                new EntreeOpa.Ressource("document", "d-1", "u-9", List.of("enseignant")),
                new EntreeOpa.Requete("GET", "/api/documents/d-1"));
    }

    @BeforeEach
    void preparer() {
        // On construit un RestClient sur un serveur HTTP simulé.
        RestClient.Builder builder = RestClient.builder().baseUrl("http://opa:8181");
        serveur = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        // Propriétés par défaut : le chemin /v1/data/unchk/authz/allow est appliqué.
        OpaProprietes proprietes = new OpaProprietes("http://opa:8181", null, null);
        opaClient = new OpaClient(restClient, proprietes);
    }

    @Test
    void appelle_la_regle_allow_avec_l_enveloppe_input_et_renvoie_la_decision() {
        // Étant donné qu'OPA répondra "autorisé"
        serveur.expect(requestTo("http://opa:8181/v1/data/unchk/authz/allow"))
                .andExpect(method(POST))
                // L'enveloppe doit contenir input.subject.id et input.action
                .andExpect(jsonPath("$.input.subject.id").value("u-123"))
                .andExpect(jsonPath("$.input.action").value("read"))
                .andExpect(jsonPath("$.input.resource.type").value("document"))
                .andRespond(withSuccess("{\"result\": true}", MediaType.APPLICATION_JSON));

        // Quand on demande la décision
        boolean autorise = opaClient.estAutorise(entreeExemple());

        // Alors l'accès est autorisé et l'appel attendu a bien eu lieu
        assertThat(autorise).isTrue();
        serveur.verify();
    }

    @Test
    void refuse_quand_opa_renvoie_result_false() {
        serveur.expect(requestTo("http://opa:8181/v1/data/unchk/authz/allow"))
                .andRespond(withSuccess("{\"result\": false}", MediaType.APPLICATION_JSON));

        // Quand OPA refuse, le client renvoie false
        assertThat(opaClient.estAutorise(entreeExemple())).isFalse();
        serveur.verify();
    }

    @Test
    void refuse_par_securite_quand_opa_est_injoignable() {
        // Étant donné qu'OPA renvoie une erreur serveur
        serveur.expect(requestTo("http://opa:8181/v1/data/unchk/authz/allow"))
                .andRespond(withServerError());

        // Quand on demande la décision, le client refuse (fail-closed)
        assertThat(opaClient.estAutorise(entreeExemple())).isFalse();
        serveur.verify();
    }
}
