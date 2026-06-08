package sn.unchk.office.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration du serveur de ressources OAuth2 pour les microservices métier (servlet).
 * <p>
 * Chaque service valide localement les jetons émis par l'identity-service en s'appuyant
 * sur le jeu de clés publiques JWKS. La validation contrôle :
 * <ul>
 *   <li>la signature RS256 (via les clés JWKS récupérées à {@code jwksUri}) ;</li>
 *   <li>l'émetteur attendu (claim {@code iss}) ;</li>
 *   <li>l'audience attendue (claim {@code aud}) ;</li>
 *   <li>l'expiration (claim {@code exp}, géré par le validateur d'horodatage).</li>
 * </ul>
 * L'API est sans état (pas de session HTTP) : l'authentification repose sur le jeton.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProprietes.class)
public class ConfigurationServeurRessources {

    private final JwtProprietes proprietes;

    public ConfigurationServeurRessources(JwtProprietes proprietes) {
        this.proprietes = proprietes;
    }

    /**
     * Chaîne de filtres de sécurité : tout endpoint exige un jeton valide,
     * sauf les sondes de santé. CSRF désactivé car API REST sans cookie de session.
     */
    @Bean
    public SecurityFilterChain chaineFiltres(HttpSecurity http) throws Exception {
        http
                // API REST sans état : pas de protection CSRF par cookie.
                .csrf(AbstractHttpConfigurer::disable)
                // Aucune session HTTP : chaque requête est authentifiée par son jeton.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorisations -> autorisations
                        // Les sondes Actuator de santé restent ouvertes au monitoring.
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Tout le reste exige une authentification valide.
                        .anyRequest().authenticated())
                // Active la validation des jetons (resource server) avec notre convertisseur.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(decodeurJwt())
                                .jwtAuthenticationConverter(convertisseur())))
                // Réponses d'erreur d'authentification/autorisation sobres (401/403 sans détail).
                .exceptionHandling(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Décodeur JWT bâti à partir du JWKS, enrichi des validateurs issuer + audience + expiration.
     */
    @Bean
    public JwtDecoder decodeurJwt() {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder
                .withJwkSetUri(proprietes.jwksUri())
                .build();
        decodeur.setJwtValidator(validateur());
        return decodeur;
    }

    /**
     * Compose le validateur global : horodatage (exp/nbf), émetteur et audience.
     * Exposé en méthode pour pouvoir être testé unitairement.
     */
    OAuth2TokenValidator<Jwt> validateur() {
        List<OAuth2TokenValidator<Jwt>> validateurs = new ArrayList<>();
        // Vérifie l'expiration (exp) et la date de validité (nbf).
        validateurs.add(new JwtTimestampValidator());
        // Vérifie l'émetteur attendu si configuré.
        if (StringUtils.hasText(proprietes.issuer())) {
            validateurs.add(new JwtIssuerValidator(proprietes.issuer()));
        }
        // Vérifie que l'audience attendue figure bien dans le claim "aud".
        if (StringUtils.hasText(proprietes.audience())) {
            validateurs.add(validateurAudience(proprietes.audience()));
        }
        return new DelegatingOAuth2TokenValidator<>(validateurs);
    }

    /**
     * Validateur d'audience : échoue si l'audience attendue n'est pas présente dans le claim {@code aud}.
     */
    static OAuth2TokenValidator<Jwt> validateurAudience(String audienceAttendue) {
        return new JwtClaimValidator<List<String>>("aud", audiences ->
                audiences != null && audiences.contains(audienceAttendue));
    }

    /**
     * Adapte notre convertisseur métier au type attendu par Spring Security.
     */
    @Bean
    public JwtAuthenticationConverter convertisseur() {
        // Spring attend un JwtAuthenticationConverter ; on délègue à notre logique d'extraction.
        ConvertisseurAuthentificationJwt delegue = new ConvertisseurAuthentificationJwt();
        JwtAuthenticationConverter adaptateur = new JwtAuthenticationConverter();
        adaptateur.setPrincipalClaimName("sub");
        // On réutilise l'extraction d'autorités du convertisseur maison.
        adaptateur.setJwtGrantedAuthoritiesConverter(jwt ->
                new ArrayList<>(delegue.convert(jwt).getAuthorities()));
        return adaptateur;
    }
}
