package sn.unchk.office.communication.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * Intercepteur de handshake WebSocket.
 * <p>
 * Au moment de l'établissement de la session, on récupère l'identité de l'utilisateur déjà
 * authentifié (le JWT a été validé en amont par le gateway puis par le serveur de ressources
 * du service). On mémorise le {@code subject.id} dans les attributs de session : il sert de
 * nom de {@link Principal} pour adresser les notifications à ce seul utilisateur (anti-IDOR
 * temps réel : pas de push cross-utilisateur).
 */
@Component
public class IntercepteurHandshakeJwt implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IntercepteurHandshakeJwt.class);

    /** Clé d'attribut de session portant l'identifiant de l'utilisateur. */
    public static final String ATTRIBUT_UTILISATEUR = "subjectId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest requete, ServerHttpResponse reponse,
                                   WebSocketHandler handler, Map<String, Object> attributs) {
        if (requete instanceof ServletServerHttpRequest servletRequete) {
            Principal principal = servletRequete.getServletRequest().getUserPrincipal();
            if (principal == null) {
                // Pas d'identité : on refuse l'ouverture de session (deny-by-default).
                log.warn("Handshake WebSocket refusé : aucune identité authentifiée");
                return false;
            }
            attributs.put(ATTRIBUT_UTILISATEUR, principal.getName());
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest requete, ServerHttpResponse reponse,
                               WebSocketHandler handler, Exception exception) {
        // Rien à faire après le handshake.
    }
}
