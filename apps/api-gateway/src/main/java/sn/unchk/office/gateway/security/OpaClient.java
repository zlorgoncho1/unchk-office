package sn.unchk.office.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client réactif d'interrogation d'OPA (Policy Decision Point).
 *
 * <p>Appelle {@code POST {OPA_URL}/v1/data/unchk/authz/allow} avec l'entrée décrivant
 * le sujet, l'action et la requête, puis renvoie la décision booléenne d'autorisation.</p>
 *
 * <p>Politique de sécurité : en cas d'erreur (OPA injoignable, délai dépassé, réponse vide),
 * on REFUSE par défaut (deny-by-default) — jamais d'ouverture en cas de doute.</p>
 */
@Component
public class OpaClient {

    private static final Logger log = LoggerFactory.getLogger(OpaClient.class);

    // Chemin de décision exposé par la politique Rego (package unchk.authz, règle allow).
    private static final String DECISION_PATH = "/v1/data/unchk/authz/allow";

    private final WebClient webClient;
    private final Duration timeout;

    public OpaClient(WebClient.Builder webClientBuilder,
                     @Value("${opa.url:http://opa:8181}") String opaUrl,
                     @Value("${opa.timeout-ms:1500}") long timeoutMs) {
        this.webClient = webClientBuilder.baseUrl(opaUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * Demande à OPA si la requête est autorisée.
     *
     * @param input description du sujet, de l'action et de la requête
     * @return {@code true} si OPA autorise explicitement, {@code false} sinon (et en cas d'erreur)
     */
    public Mono<Boolean> isAllowed(OpaInput input) {
        return webClient.post()
                .uri(DECISION_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                // OPA attend l'objet enveloppé dans {"input": {...}}.
                .bodyValue(new OpaInput.Body(input))
                .retrieve()
                .bodyToMono(OpaResult.class)
                .timeout(timeout)
                .map(OpaResult::allowed)
                // Toute erreur (réseau, délai, 5xx) => refus par défaut.
                .onErrorResume(error -> {
                    log.warn("Décision OPA indisponible, refus par défaut : {}", error.getMessage());
                    return Mono.just(false);
                })
                // Réponse vide => refus par défaut.
                .defaultIfEmpty(false);
    }
}
