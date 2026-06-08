package sn.unchk.office.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Configuration de sécurité RÉACTIVE de la passerelle.
 *
 * <p>Rôle :</p>
 * <ul>
 *   <li>Active le serveur de ressources OAuth2 réactif : tout JWT est validé
 *       (signature RS256, issuer, expiration) à partir des clés publiques JWKS d'identity-service.</li>
 *   <li>Applique un CORS en liste blanche (origine du frontend uniquement).</li>
 *   <li>Refus par défaut (deny-by-default) : toute requête doit être authentifiée,
 *       sauf les endpoints publics explicitement listés (santé, actuator, JWKS local éventuel).</li>
 * </ul>
 *
 * <p>L'autorisation fine RBAC (rôle × route) est déléguée à OPA dans un {@code GlobalFilter}
 * dédié ({@link sn.unchk.office.gateway.filter.OpaAuthorizationFilter}) : Spring Security
 * ne fait ici que garantir l'authentification.</p>
 */
@Configuration
public class SecurityConfig {

    // Origine autorisée du frontend Angular (liste blanche CORS).
    private final String frontendOrigin;

    public SecurityConfig(org.springframework.core.env.Environment env) {
        // Lue depuis la configuration (cors.allowed-origin) avec repli sur le front local.
        this.frontendOrigin = env.getProperty("cors.allowed-origin", "http://localhost:4200");
    }

    /**
     * Chaîne de filtres de sécurité réactive.
     *
     * <p>On désactive CSRF (API stateless sans cookie de session : protégée par JWT)
     * et on impose l'authentification JWT partout, hormis les chemins publics.</p>
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // CORS en liste blanche (voir corsConfigurationSource).
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // API stateless protégée par jeton : pas de protection CSRF par cookie nécessaire.
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Pas d'authentification HTTP Basic ni de formulaire de connexion sur la passerelle.
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // Autorisations au niveau transport : refus par défaut.
            .authorizeExchange(exchanges -> exchanges
                // Pré-vols CORS toujours autorisés.
                .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // Endpoints publics de supervision (sondes Docker).
                .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Authentification (connexion / rafraîchissement) : publique, pas de JWT requis.
                .pathMatchers("/api/identity/auth/**").permitAll()
                // Tout le reste exige un JWT valide.
                .anyExchange().authenticated()
            )
            // Validation JWT via serveur de ressources OAuth2 réactif (clés JWKS).
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // Réponses d'erreur sobres : 401 si non authentifié, 403 si interdit (pas de fuite d'info).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            );

        return http.build();
    }

    /**
     * Convertisseur réactif transformant le JWT en authentification Spring.
     *
     * <p>On délègue l'extraction des rôles au {@link JwtRolesConverter} (claim "roles").
     * Ces rôles seront ensuite réutilisés par le filtre OPA pour le RBAC.</p>
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(new JwtRolesConverter());
    }

    /**
     * Politique CORS en liste blanche : seule l'origine du frontend est acceptée.
     *
     * <p>On autorise les méthodes REST usuelles, les en-têtes courants (dont Authorization)
     * et l'en-tête de corrélation, et on expose l'en-tête de corrélation au navigateur.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Liste blanche stricte : pas de "*".
        config.setAllowedOrigins(List.of(frontendOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        // Cookies/identifiants autorisés (jeton porté par en-tête Authorization).
        config.setAllowCredentials(true);
        // Durée de mise en cache des pré-vols CORS (en secondes).
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
