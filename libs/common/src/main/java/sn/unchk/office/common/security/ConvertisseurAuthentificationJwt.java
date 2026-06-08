package sn.unchk.office.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convertit un {@link Jwt} validé en {@link AbstractAuthenticationToken} pour le SecurityContext.
 * <p>
 * Deux informations sont extraites du jeton émis par l'identity-service :
 * <ul>
 *   <li>les rôles (claim {@code roles}), transformés en autorités préfixées {@code ROLE_} ;</li>
 *   <li>l'identifiant de l'utilisateur (claim {@code sub}), exposé comme nom du principal.</li>
 * </ul>
 * Les rôles du projet sont : admin, administratif, enseignant, appui-insertion, etudiant.
 */
public class ConvertisseurAuthentificationJwt
        implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Nom du claim contenant la liste des rôles dans nos jetons. */
    private static final String CLAIM_ROLES = "roles";

    /** Préfixe imposé par Spring Security pour les autorités de type rôle. */
    private static final String PREFIXE_ROLE = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> autorites = extraireAutorites(jwt);
        // Le claim "sub" identifie l'utilisateur : on le fixe comme nom du principal.
        String userId = jwt.getSubject();
        return new JwtAuthenticationToken(jwt, autorites, userId);
    }

    /**
     * Transforme le claim {@code roles} en autorités Spring Security.
     * Tolère l'absence du claim (jeton sans rôle) en renvoyant une liste vide.
     */
    private Collection<GrantedAuthority> extraireAutorites(Jwt jwt) {
        List<String> roles = lireRoles(jwt);
        Collection<GrantedAuthority> autorites = new ArrayList<>();
        for (String role : roles) {
            // On évite un double préfixe si le rôle est déjà préfixé en amont.
            String autorite = role.startsWith(PREFIXE_ROLE) ? role : PREFIXE_ROLE + role;
            autorites.add(new SimpleGrantedAuthority(autorite));
        }
        return autorites;
    }

    /**
     * Lit le claim {@code roles} de façon défensive : il peut être absent ou typé
     * en collection hétérogène selon la sérialisation. On ignore les entrées non textuelles.
     */
    @SuppressWarnings("unchecked")
    private List<String> lireRoles(Jwt jwt) {
        Object brut = jwt.getClaims().get(CLAIM_ROLES);
        if (brut instanceof Collection<?> collection) {
            List<String> roles = new ArrayList<>();
            for (Object element : collection) {
                if (element != null) {
                    roles.add(element.toString());
                }
            }
            return roles;
        }
        return List.of();
    }
}
