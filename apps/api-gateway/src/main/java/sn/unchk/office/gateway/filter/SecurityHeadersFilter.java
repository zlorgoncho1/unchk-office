package sn.unchk.office.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre ajoutant les en-têtes de sécurité HTTP sur TOUTES les réponses.
 *
 * <p>Pré-blindage OWASP (durcissement des en-têtes), mutualisé au niveau de la passerelle :</p>
 * <ul>
 *   <li><b>Content-Security-Policy</b> : restreint les sources de contenu (anti-XSS/injection).</li>
 *   <li><b>Strict-Transport-Security</b> : force HTTPS sur la durée (anti-downgrade).</li>
 *   <li><b>X-Frame-Options</b> : interdit l'inclusion en iframe (anti-clickjacking).</li>
 *   <li><b>X-Content-Type-Options</b> : empêche le MIME-sniffing.</li>
 *   <li><b>Referrer-Policy</b> : limite les informations de provenance fuitées.</li>
 *   <li><b>Permissions-Policy</b> : désactive les API navigateur sensibles non utilisées.</li>
 * </ul>
 *
 * <p>Les en-têtes sont posés juste avant l'envoi de la réponse (beforeCommit) pour
 * être présents quelle que soit l'issue (succès, refus, erreur).</p>
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // On enregistre l'ajout des en-têtes juste avant la validation de la réponse.
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // API JSON uniquement : politique CSP restrictive (rien d'externe par défaut).
            ajouter(headers, "Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            // Force HTTPS pendant 1 an, sous-domaines inclus, éligible au préchargement.
            ajouter(headers, "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
            // Interdit l'affichage dans une iframe (clickjacking).
            ajouter(headers, "X-Frame-Options", "DENY");
            // Empêche le navigateur de deviner le type MIME.
            ajouter(headers, "X-Content-Type-Options", "nosniff");
            // Ne transmet jamais le referer vers une origine différente.
            ajouter(headers, "Referrer-Policy", "no-referrer");
            // Désactive les API navigateur sensibles (géoloc, caméra, micro).
            ajouter(headers, "Permissions-Policy",
                    "geolocation=(), camera=(), microphone=()");

            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    /** Pose l'en-tête seulement s'il n'est pas déjà présent (n'écrase pas un service en aval). */
    private void ajouter(HttpHeaders headers, String nom, String valeur) {
        if (!headers.containsKey(nom)) {
            headers.set(nom, valeur);
        }
    }

    /**
     * Ordre très bas (priorité haute) pour s'exécuter tôt et garantir l'enregistrement
     * du callback beforeCommit avant tout autre traitement.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
