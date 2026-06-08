package sn.unchk.office.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtre de propagation d'un identifiant de corrélation.
 *
 * <p>À l'entrée de la passerelle, on s'assure que chaque requête porte un en-tête
 * {@code X-Correlation-Id} : on reprend celui fourni par le client s'il existe,
 * sinon on en génère un (UUID). Cet identifiant est :</p>
 * <ul>
 *   <li>réinjecté dans la requête transmise au microservice en aval (traçabilité bout-en-bout) ;</li>
 *   <li>renvoyé dans la réponse au client (corrélation côté frontend / support).</li>
 * </ul>
 *
 * <p>S'exécute très tôt pour que tous les autres filtres (dont les journaux) disposent
 * de l'identifiant.</p>
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    // Nom standard de l'en-tête de corrélation utilisé dans toute la plateforme.
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Reprend l'identifiant fourni par le client, sinon en génère un nouveau.
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String idFinal = correlationId;

        // Propage l'identifiant vers le service en aval.
        ServerHttpRequest requeteMutee = exchange.getRequest().mutate()
                .header(CORRELATION_HEADER, idFinal)
                .build();

        // Le renvoie aussi au client, mais via beforeCommit (juste avant l'envoi) :
        // modifier la réponse de façon synchrone AVANT le routage fige son état et fait
        // perdre le corps proxifié par Spring Cloud Gateway (corps vide / content-length 0).
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(CORRELATION_HEADER, idFinal);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(requeteMutee).build());
    }

    /** Doit s'exécuter parmi les tout premiers filtres globaux. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
