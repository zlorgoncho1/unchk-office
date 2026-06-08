package sn.unchk.office.communication.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des sessions WebSocket de notifications.
 * <p>
 * Maintient une table {@code userId -> sessions ouvertes} (un utilisateur peut avoir
 * plusieurs onglets) et permet de pousser un objet JSON à un destinataire ciblé.
 * L'identité provient des attributs de session posés par {@link IntercepteurHandshakeJwt}.
 */
@Component
public class GestionnaireNotificationsWs extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireNotificationsWs.class);

    // userId -> ensemble de sessions ouvertes.
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public GestionnaireNotificationsWs(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        if (userId == null) {
            fermerSilencieusement(session);
            return;
        }
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session WS ouverte (user={}, sessions={})", userId, sessions.get(userId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> ouvertes = sessions.get(userId);
        if (ouvertes != null) {
            ouvertes.remove(session);
            if (ouvertes.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    /**
     * Pousse un objet (sérialisé en JSON) à toutes les sessions ouvertes d'un utilisateur.
     *
     * @return {@code true} si au moins une session a reçu le message.
     */
    public boolean pousser(String userId, Object payload) {
        Set<WebSocketSession> ouvertes = sessions.get(userId);
        if (ouvertes == null || ouvertes.isEmpty()) {
            return false;
        }
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            boolean envoye = false;
            for (WebSocketSession s : ouvertes) {
                if (s.isOpen()) {
                    // L'envoi sur une session WebSocket doit être sérialisé (non thread-safe).
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                    envoye = true;
                }
            }
            return envoye;
        } catch (IOException e) {
            log.warn("Échec d'envoi WS (user={}) : {}", userId, e.getMessage());
            return false;
        }
    }

    private String userId(WebSocketSession session) {
        Object v = session.getAttributes().get(IntercepteurHandshakeJwt.ATTRIBUT_UTILISATEUR);
        return v != null ? v.toString() : null;
    }

    private void fermerSilencieusement(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
            // Sans effet : la session est déjà en cours de fermeture.
        }
    }
}
