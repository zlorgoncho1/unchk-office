package sn.unchk.office.communication.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Intercepteur de handshake WebSocket : authentifie l'ouverture de session via le JWT
 * transmis en paramètre d'URL ({@code ?access_token=...}). On valide le jeton
 * (signature / émetteur / expiration) avec le même décodeur que les API REST, puis on
 * mémorise l'identifiant de l'utilisateur ({@code sub}) dans les attributs de session :
 * il sert à adresser les notifications à ce seul utilisateur (anti-IDOR temps réel).
 */
@Component
public class IntercepteurHandshakeJwt implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IntercepteurHandshakeJwt.class);

    /** Clé d'attribut de session portant l'identifiant de l'utilisateur. */
    public static final String ATTRIBUT_UTILISATEUR = "subjectId";
    private static final String PARAM_TOKEN = "access_token";

    private final JwtDecoder jwtDecoder;

    public IntercepteurHandshakeJwt(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest requete, ServerHttpResponse reponse,
                                   WebSocketHandler handler, Map<String, Object> attributs) {
        String token = jetonDepuisUrl(requete);
        if (token == null || token.isBlank()) {
            log.warn("Handshake WebSocket refusé : jeton absent");
            return false;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributs.put(ATTRIBUT_UTILISATEUR, jwt.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("Handshake WebSocket refusé : jeton invalide ({})", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest requete, ServerHttpResponse reponse,
                               WebSocketHandler handler, Exception exception) {
        // Rien à faire après le handshake.
    }

    /** Extrait et décode le paramètre access_token de l'URL du handshake. */
    private String jetonDepuisUrl(ServerHttpRequest requete) {
        String brut = UriComponentsBuilder.fromUri(requete.getURI()).build()
                .getQueryParams().getFirst(PARAM_TOKEN);
        return brut != null ? URLDecoder.decode(brut, StandardCharsets.UTF_8) : null;
    }
}
