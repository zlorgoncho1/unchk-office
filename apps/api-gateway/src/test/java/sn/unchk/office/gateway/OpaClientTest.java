package sn.unchk.office.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import sn.unchk.office.gateway.security.OpaClient;
import sn.unchk.office.gateway.security.OpaInput;

import java.util.List;

/**
 * Tests unitaires du client OPA (autorisation RBAC).
 *
 * <p>On vérifie les trois comportements clés exigés par la sécurité de la passerelle :</p>
 * <ul>
 *   <li>OPA répond "allow" => autorisé ;</li>
 *   <li>OPA répond "deny" => refusé ;</li>
 *   <li>OPA injoignable / en erreur => REFUSÉ par défaut (deny-by-default).</li>
 * </ul>
 *
 * <p>On injecte un {@link ExchangeFunction} simulé dans le {@link WebClient.Builder}
 * pour ne pas dépendre d'un vrai serveur OPA.</p>
 */
class OpaClientTest {

    // Entrée d'exemple : un enseignant fait un GET sur /api/academic.
    private final OpaInput entreeExemple = new OpaInput(
            new OpaInput.Subject("u-123", List.of("enseignant")),
            "read",
            new OpaInput.Resource("academic"),
            new OpaInput.Request("GET", "/api/academic/formations")
    );

    /** Construit un OpaClient dont le WebClient renvoie toujours la réponse JSON fournie. */
    private OpaClient clientAvecReponse(String corpsJson) {
        ExchangeFunction exchange = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(corpsJson)
                        .build()
        );
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        return new OpaClient(builder, "http://opa:8181", 1500);
    }

    /** Construit un OpaClient dont l'appel OPA échoue systématiquement. */
    private OpaClient clientEnErreur() {
        ExchangeFunction exchange = request -> Mono.error(new RuntimeException("OPA injoignable"));
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        return new OpaClient(builder, "http://opa:8181", 1500);
    }

    @Test
    @DisplayName("OPA autorise (result=true) => requête permise")
    void autoriseQuandOpaAccepte() {
        OpaClient client = clientAvecReponse("{\"result\": true}");

        StepVerifier.create(client.isAllowed(entreeExemple))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("OPA refuse (result=false) => requête interdite")
    void refuseQuandOpaRefuse() {
        OpaClient client = clientAvecReponse("{\"result\": false}");

        StepVerifier.create(client.isAllowed(entreeExemple))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("OPA injoignable => refus par défaut (deny-by-default)")
    void refuseParDefautSiOpaEnErreur() {
        OpaClient client = clientEnErreur();

        StepVerifier.create(client.isAllowed(entreeExemple))
                .expectNext(false)
                .verifyComplete();
    }
}
