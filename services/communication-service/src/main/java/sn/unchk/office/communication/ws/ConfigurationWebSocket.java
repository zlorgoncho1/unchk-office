package sn.unchk.office.communication.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration du canal WebSocket (simple, messages texte/JSON) des notifications temps réel.
 * <p>
 * Endpoint : {@code /ws/notifications} (routé par le gateway). Le handshake est authentifié
 * par le JWT transmis en paramètre d'URL ({@code ?access_token=...}), car le navigateur ne
 * peut pas poser d'en-tête {@code Authorization} à l'ouverture d'un WebSocket. Chaque
 * notification est ensuite poussée en JSON direct à la (les) session(s) du destinataire
 * (lié à son {@code subject.id} : pas de push cross-utilisateur — anti-IDOR temps réel).
 */
@Configuration
@EnableWebSocket
public class ConfigurationWebSocket implements WebSocketConfigurer {

    private final GestionnaireNotificationsWs gestionnaire;
    private final IntercepteurHandshakeJwt intercepteurHandshake;

    public ConfigurationWebSocket(GestionnaireNotificationsWs gestionnaire,
                                  IntercepteurHandshakeJwt intercepteurHandshake) {
        this.gestionnaire = gestionnaire;
        this.intercepteurHandshake = intercepteurHandshake;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gestionnaire, "/ws/notifications")
                .addInterceptors(intercepteurHandshake)
                // Le contrôle d'origine (CORS) est assuré au gateway.
                .setAllowedOriginPatterns("*");
    }
}
