package sn.unchk.office.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convertit un JWT (émis par identity-service) en authentification Spring,
 * en extrayant les rôles depuis le claim "roles".
 *
 * <p>Les rôles UNCHK (admin, administratif, enseignant, appui-insertion, etudiant)
 * sont exposés à la fois comme autorités Spring ("ROLE_xxx") et, plus tard, transmis
 * tels quels à OPA pour la décision RBAC (rôle × route).</p>
 */
public class JwtRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    // Nom du claim portant la liste des rôles dans le JWT.
    private static final String CLAIM_ROLES = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt).stream()
                // Convention Spring : préfixe ROLE_ pour les autorités issues de rôles.
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Le "name" de l'authentification = sujet du jeton (identifiant utilisateur UUID).
        return new UsernamePasswordAuthenticationToken(jwt, jwt, authorities);
    }

    /**
     * Lit le claim "roles" de façon défensive (liste, chaîne unique, ou absent).
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object raw = jwt.getClaim(CLAIM_ROLES);
        if (raw instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.toList());
        }
        if (raw instanceof String single && !single.isBlank()) {
            return List.of(single);
        }
        // Aucun rôle déclaré : liste vide (OPA refusera par défaut).
        return List.of();
    }
}
