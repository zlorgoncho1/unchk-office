package sn.unchk.office.identity.securite;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import sn.unchk.office.common.security.ConvertisseurAuthentificationJwt;
import sn.unchk.office.common.security.JwtProprietes;

import java.util.ArrayList;

/**
 * Configuration de sécurité de l'identity-service (serveur de ressources + endpoints publics).
 * <p>
 * Remplace la chaîne de sécurité générique de {@code libs/common} (qui n'ouvre que la sonde de
 * santé) : l'identity-service étant l'autorité d'émission, ses endpoints d'authentification et
 * son JWKS doivent rester PUBLICS (un client n'a pas encore de jeton au moment du login).
 * Les endpoints de gestion des comptes exigent un jeton valide et le rôle {@code admin}
 * (vérifié au niveau méthode via {@code @PreAuthorize}).
 * <p>
 * Pour éviter le conflit de deux chaînes de filtres, l'auto-configuration de sécurité de
 * {@code common} ({@code ConfigurationServeurRessources}) est exclue (cf. {@code application.yml}
 * — {@code spring.autoconfigure.exclude}). On réutilise toutefois le décodeur JWKS et le
 * convertisseur d'autorités fournis par {@code common}.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProprietes.class, JwtEmissionProprietes.class, SecuriteProprietes.class})
public class ConfigurationSecuriteIdentity {

    private final JwtProprietes proprietesJwt;

    public ConfigurationSecuriteIdentity(JwtProprietes proprietesJwt) {
        this.proprietesJwt = proprietesJwt;
    }

    /** Encodeur BCrypt pour hacher et vérifier les mots de passe (jamais en clair). */
    @Bean
    public PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Décodeur JWT bâti à partir du JWKS local : valide la signature RS256 des jetons reçus
     * sur les endpoints protégés (défense en profondeur, en plus de la validation au gateway).
     */
    @Bean
    public JwtDecoder decodeurJwt() {
        return NimbusJwtDecoder.withJwkSetUri(proprietesJwt.jwksUri()).build();
    }

    /** Convertisseur d'autorités : extrait les rôles (claim {@code roles}) et le sujet (claim {@code sub}). */
    @Bean
    public JwtAuthenticationConverter convertisseur() {
        ConvertisseurAuthentificationJwt delegue = new ConvertisseurAuthentificationJwt();
        JwtAuthenticationConverter adaptateur = new JwtAuthenticationConverter();
        adaptateur.setPrincipalClaimName("sub");
        adaptateur.setJwtGrantedAuthoritiesConverter(jwt ->
                new ArrayList<>(delegue.convert(jwt).getAuthorities()));
        return adaptateur;
    }

    /**
     * Chaîne de filtres unique : endpoints publics d'auth/JWKS + le reste authentifié.
     */
    @Bean
    public SecurityFilterChain chaineFiltres(HttpSecurity http,
                                             JwtDecoder decodeurJwt,
                                             JwtAuthenticationConverter convertisseur) throws Exception {
        http
                // API REST sans état : pas de protection CSRF par cookie.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorisations -> autorisations
                        // Endpoints publics : connexion, rafraîchissement, déconnexion.
                        .requestMatchers(HttpMethod.POST,
                                "/api/identity/auth/login",
                                "/api/identity/auth/refresh",
                                "/api/identity/auth/logout").permitAll()
                        // JWKS exposé en deux emplacements (direct + sous /api/identity pour le gateway).
                        .requestMatchers(HttpMethod.GET,
                                "/.well-known/jwks.json",
                                "/api/identity/auth/.well-known/jwks.json").permitAll()
                        // Sondes Actuator ouvertes au monitoring.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Tout le reste (gestion des comptes) exige un jeton valide.
                        .anyRequest().authenticated())
                // Serveur de ressources : valide les jetons reçus pour les endpoints protégés.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(decodeurJwt)
                                .jwtAuthenticationConverter(convertisseur)))
                .exceptionHandling(Customizer.withDefaults());
        return http.build();
    }
}
