package sn.unchk.office.communication.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration du canal WebSocket / STOMP des notifications temps réel.
 * <p>
 * Endpoint d'établissement de session : {@code /ws/notifications} (le gateway y route le
 * handshake après validation du JWT). Le push se fait sur une file utilisateur :
 * chaque destinataire reçoit ses notifications sur {@code /user/queue/notifications}, lié à
 * son {@code subject.id} (pas d'IDOR temps réel cross-utilisateur).
 */
@Configuration
@EnableWebSocketMessageBroker
public class ConfigurationWebSocket implements WebSocketMessageBrokerConfigurer {

    /** Préfixe des destinations utilisateur (push ciblé par session). */
    public static final String PREFIXE_UTILISATEUR = "/user";
    /** Destination de file des notifications poussées au client. */
    public static final String DESTINATION_NOTIFICATIONS = "/queue/notifications";

    private final IntercepteurHandshakeJwt intercepteurHandshake;
    private final GestionnaireHandshakeUtilisateur gestionnaireHandshake;

    public ConfigurationWebSocket(IntercepteurHandshakeJwt intercepteurHandshake,
                                  GestionnaireHandshakeUtilisateur gestionnaireHandshake) {
        this.intercepteurHandshake = intercepteurHandshake;
        this.gestionnaireHandshake = gestionnaireHandshake;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de handshake ; l'intercepteur lie la session à l'identité de l'utilisateur,
        // le gestionnaire en fait le Principal STOMP (routage /user/...).
        registry.addEndpoint("/ws/notifications")
                .addInterceptors(intercepteurHandshake)
                .setHandshakeHandler(gestionnaireHandshake)
                .setAllowedOriginPatterns("*"); // le contrôle d'origine (CORS) est assuré au gateway
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Courtier simple en mémoire : suffisant pour un push mono-instance.
        registry.enableSimpleBroker(DESTINATION_NOTIFICATIONS);
        // Préfixe des destinations propres à un utilisateur (résolu via son Principal).
        registry.setUserDestinationPrefix(PREFIXE_UTILISATEUR);
    }
}
