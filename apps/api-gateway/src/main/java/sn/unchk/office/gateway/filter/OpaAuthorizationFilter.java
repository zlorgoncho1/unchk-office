package sn.unchk.office.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sn.unchk.office.gateway.security.OpaClient;
import sn.unchk.office.gateway.security.OpaInput;

import java.util.List;

/**
 * Filtre global d'autorisation RBAC délégué à OPA (rôle × route).
 *
 * <p>Le contexte de sécurité réactif n'est PAS propagé de façon fiable aux GlobalFilters
 * de Spring Cloud Gateway (ni {@code ReactiveSecurityContextHolder}, ni
 * {@code exchange.getPrincipal()} ne portent l'authentification ici). On décode donc le JWT
 * — déjà validé en amont par le resource server — pour en extraire le sujet et les rôles,
 * puis on interroge OPA. Principe deny-by-default : pas de jeton, jeton invalide,
 * décision négative ou OPA injoignable => 403, sans détail interne.</p>
 */
@Component
public class OpaAuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthorizationFilter.class);

    // Claim du JWT portant la liste des rôles.
    private static final String CLAIM_ROLES = "roles";

    // Chemins publics non soumis à l'autorisation OPA (alignés sur SecurityConfig).
    private static final List<String> CHEMINS_PUBLICS = List.of(
            "/actuator/health", "/actuator/info", "/api/identity/auth", "/ws/notifications"
    );

    private final OpaClient opaClient;
    private final ReactiveJwtDecoder jwtDecoder;

    public OpaAuthorizationFilter(OpaClient opaClient, ReactiveJwtDecoder jwtDecoder) {
        this.opaClient = opaClient;
        this.jwtDecoder = jwtDecoder;
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

        String token = jetonBearer(request);
        if (token == null) {
            return refuser(exchange, "jeton Bearer absent");
        }

        // Décodage du JWT (déjà validé) -> sujet + rôles -> décision OPA.
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    String subjectId = jwt.getSubject();
                    List<String> roles = lireRoles(jwt);
                    OpaInput input = construireEntree(subjectId, roles, method, path);

                    // On ne route que si la décision OPA est "allow".
                    return opaClient.isAllowed(input).flatMap(allowed -> {
                        if (Boolean.TRUE.equals(allowed)) {
                            return chain.filter(exchange);
                        }
                        return refuser(exchange, "autorisation OPA refusée pour " + method + " " + path);
                    });
                })
                .onErrorResume(e -> refuser(exchange, "jeton invalide"));
    }

    /** Extrait le jeton du header Authorization: Bearer &lt;jwt&gt;. */
    private String jetonBearer(ServerHttpRequest request) {
        String h = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String t = h.substring(7).trim();
            return t.isEmpty() ? null : t;
        }
        return null;
    }

    /** Lit la liste des rôles depuis le claim "roles" (vide si absent). */
    private List<String> lireRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);
        return roles != null ? roles : List.of();
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
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2];
        }
        return "";
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
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
