package sn.unchk.office.communication.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chaîne de sécurité dédiée au handshake WebSocket des notifications.
 * <p>
 * Le navigateur ne peut pas poser d'en-tête {@code Authorization} à l'ouverture d'un
 * WebSocket : le JWT est passé en {@code ?access_token} et validé par
 * {@link IntercepteurHandshakeJwt} au handshake. Cette chaîne, en priorité HAUTE et limitée
 * à {@code /ws/**}, laisse donc passer le handshake (l'authentification réelle est faite par
 * l'intercepteur). Les autres requêtes ne correspondent pas à ce matcher et restent
 * protégées par la chaîne JWT du resource server.
 */
@Configuration
public class ConfigurationSecuriteWebSocket {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain chaineWebSocket(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ws/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorisations -> autorisations.anyRequest().permitAll());
        return http.build();
    }
}
