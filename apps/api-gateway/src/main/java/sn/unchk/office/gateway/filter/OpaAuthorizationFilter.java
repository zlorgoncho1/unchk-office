package sn.unchk.office.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sn.unchk.office.gateway.security.OpaClient;
import sn.unchk.office.gateway.security.OpaInput;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filtre global d'autorisation RBAC délégué à OPA (rôle × route).
 *
 * <p>Pour chaque requête authentifiée, ce filtre :</p>
 * <ol>
 *   <li>récupère le contexte de sécurité réactif (JWT déjà validé par Spring Security) ;</li>
 *   <li>construit l'entrée OPA (sujet + rôles, action déduite de la méthode HTTP, chemin) ;</li>
 *   <li>interroge OPA et laisse passer SEULEMENT si la décision est "allow".</li>
 * </ol>
 *
 * <p>Principe deny-by-default : absence d'authentification, absence de décision,
 * ou OPA injoignable => 403 Forbidden, sans détail interne.</p>
 *
 * <p>Ce filtre s'exécute APRÈS la validation JWT de Spring Security et APRÈS les
 * filtres de corrélation / en-têtes, mais AVANT le routage effectif vers le service.</p>
 */
@Component
public class OpaAuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthorizationFilter.class);

    // Préfixe ajouté aux rôles par Spring Security (retiré avant l'envoi à OPA).
    private static final String ROLE_PREFIX = "ROLE_";

    // Chemins publics non soumis à l'autorisation OPA (alignés sur SecurityConfig).
    private static final List<String> CHEMINS_PUBLICS = List.of(
            "/actuator/health", "/actuator/info"
    );

    private final OpaClient opaClient;

    public OpaAuthorizationFilter(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();
        String method = request.getMethod().name();

        // Les chemins publics et les pré-vols CORS (OPTIONS) ne passent pas par OPA.
        if (estPublic(path) || "OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // On récupère le JWT validé depuis le contexte de sécurité réactif.
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(authentication -> {
                    if (authentication == null || !authentication.isAuthenticated()) {
                        return refuser(exchange, "non authentifié");
                    }

                    // Le principal est le JWT (cf. JwtRolesConverter) : on en tire le sujet (UUID).
                    String subjectId = authentication.getName();
                    if (authentication.getPrincipal() instanceof Jwt jwt) {
                        subjectId = jwt.getSubject();
                    }

                    List<String> roles = extraireRoles(authentication.getAuthorities());

                    OpaInput input = construireEntree(subjectId, roles, method, path);

                    // Appel OPA : on ne route que si la décision est "allow".
                    return opaClient.isAllowed(input).flatMap(allowed -> {
                        if (Boolean.TRUE.equals(allowed)) {
                            return chain.filter(exchange);
                        }
                        return refuser(exchange, "autorisation OPA refusée pour " + method + " " + path);
                    });
                })
                // Aucun contexte de sécurité => non authentifié => refus.
                .switchIfEmpty(refuser(exchange, "contexte de sécurité absent"));
    }

    /** Construit l'entrée OPA conforme à la politique unchk.authz. */
    private OpaInput construireEntree(String subjectId, List<String> roles, String method, String path) {
        OpaInput.Subject subject = new OpaInput.Subject(subjectId, roles);
        OpaInput.Resource resource = new OpaInput.Resource(typeRessource(path));
        OpaInput.Request req = new OpaInput.Request(method, path);
        return new OpaInput(subject, actionDepuisMethode(method), resource, req);
    }

    /** Déduit l'action métier (read/create/update/delete) à partir de la méthode HTTP. */
    private String actionDepuisMethode(String method) {
        return switch (method.toUpperCase()) {
            case "GET", "HEAD" -> "read";
            case "POST" -> "create";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "read";
        };
    }

    /**
     * Déduit le "type" de ressource depuis le chemin (2e segment après /api/...).
     * Ex. /api/documents/d-1 => "documents". Sert d'indice à OPA (non bloquant pour le RBAC).
     */
    private String typeRessource(String path) {
        String[] segments = path.split("/");
        // segments[0] = "", segments[1] = "api", segments[2] = ressource.
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2];
        }
        return "";
    }

    /** Retire le préfixe ROLE_ pour transmettre à OPA les rôles UNCHK bruts. */
    private List<String> extraireRoles(java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith(ROLE_PREFIX) ? a.substring(ROLE_PREFIX.length()) : a)
                .collect(Collectors.toList());
    }

    /** Termine la requête en 403 (refus) sans divulguer de détail interne au client. */
    private Mono<Void> refuser(ServerWebExchange exchange, String raison) {
        log.debug("Accès refusé : {}", raison);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean estPublic(String path) {
        return CHEMINS_PUBLICS.stream().anyMatch(path::startsWith);
    }

    /**
     * Ordre d'exécution : juste après l'authentification mais avant le routage.
     * Une valeur basse (mais positive) le place tôt dans la chaîne des filtres globaux.
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
