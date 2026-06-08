package sn.unchk.office.gateway.filter;

import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de limitation de débit (rate-limiting) simple, en mémoire.
 *
 * <p>S'appuie sur bucket4j-core (algorithme du seau à jetons / token-bucket).
 * Chaque clé d'appel (sujet authentifié si présent, sinon adresse IP) dispose de son
 * propre seau : on autorise un débit soutenu avec une petite réserve de pics.</p>
 *
 * <p>Implémentation volontairement locale (mémoire JVM) : suffisante pour une passerelle
 * mono-instance de démonstration. En production multi-instances, on brancherait un backend
 * partagé (ex. Redis) sans changer la logique de ce filtre.</p>
 *
 * <p>Au dépassement, on répond 429 Too Many Requests avec l'en-tête {@code Retry-After}.</p>
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    // Registre des seaux par clé d'appel (IP ou sujet). Nettoyé naturellement par le GC en démo.
    private final ConcurrentHashMap<String, Bucket> seaux = new ConcurrentHashMap<>();

    // Nombre de requêtes autorisées par fenêtre (débit nominal).
    private final long capacite;
    // Durée de la fenêtre de recharge.
    private final Duration fenetre;

    public RateLimitFilter(@Value("${gateway.rate-limit.capacity:100}") long capacite,
                           @Value("${gateway.rate-limit.window-seconds:60}") long fenetreSecondes) {
        this.capacite = capacite;
        this.fenetre = Duration.ofSeconds(fenetreSecondes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // On préfère limiter par sujet authentifié ; à défaut, par adresse IP.
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication() != null ? ctx.getAuthentication().getName() : null)
                .defaultIfEmpty("")
                .flatMap(sujet -> {
                    String cle = (sujet != null && !sujet.isBlank()) ? "sub:" + sujet : "ip:" + adresseIp(exchange);
                    Bucket seau = seaux.computeIfAbsent(cle, k -> creerSeau());

                    // Tente de consommer un jeton : true = autorisé, false = quota dépassé.
                    if (seau.tryConsume(1)) {
                        return chain.filter(exchange);
                    }
                    return tropDeRequetes(exchange);
                });
    }

    /** Crée un seau neuf : capacité fixe rechargée progressivement sur la durée de la fenêtre. */
    private Bucket creerSeau() {
        // API bucket4j 8.x : configuration de la bande passante via lambda (capacity + refillGreedy).
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(capacite).refillGreedy(capacite, fenetre))
                .build();
    }

    /** Récupère l'adresse IP du client (prend l'adresse distante de la connexion). */
    private String adresseIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "inconnue";
    }

    /** Répond 429 avec un en-tête Retry-After indiquant la fenêtre de recharge. */
    private Mono<Void> tropDeRequetes(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(fenetre.toSeconds()));
        return exchange.getResponse().setComplete();
    }

    /**
     * S'exécute après la corrélation et les en-têtes, mais avant l'autorisation OPA :
     * inutile d'évaluer une politique si le client est déjà au-delà de son quota.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
