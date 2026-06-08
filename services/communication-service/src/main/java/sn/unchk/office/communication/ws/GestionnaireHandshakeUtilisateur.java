package sn.unchk.office.communication.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Détermine le {@link Principal} d'une session WebSocket à partir de l'identifiant
 * d'utilisateur posé par {@link IntercepteurHandshakeJwt}.
 * <p>
 * Ce principal nommé permet d'adresser un message à un destinataire précis via les
 * destinations utilisateur STOMP ({@code /user/queue/notifications}).
 */
@Component
public class GestionnaireHandshakeUtilisateur extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest requete, WebSocketHandler handler,
                                      Map<String, Object> attributs) {
        Object id = attributs.get(IntercepteurHandshakeJwt.ATTRIBUT_UTILISATEUR);
        if (id == null) {
            return null;
        }
        String nom = id.toString();
        // Principal minimal : seul le nom (subject.id) est nécessaire au routage utilisateur.
        return () -> nom;
    }
}
