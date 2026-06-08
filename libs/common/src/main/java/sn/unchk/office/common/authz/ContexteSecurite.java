package sn.unchk.office.common.authz;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaires de lecture du contexte de sécurité courant.
 * <p>
 * Centralise l'extraction de l'identifiant de l'utilisateur (claim {@code sub}, exposé
 * comme nom du principal) et de ses rôles, afin de construire le sujet OPA.
 */
public final class ContexteSecurite {

    /** Préfixe que Spring Security ajoute aux autorités de type rôle. */
    private static final String PREFIXE_ROLE = "ROLE_";

    private ContexteSecurite() {
        // Classe utilitaire : pas d'instanciation.
    }

    /**
     * Construit le sujet OPA à partir de l'authentification courante.
     *
     * @return sujet (id + rôles) ; id et liste de rôles peuvent être vides si non authentifié
     */
    public static EntreeOpa.Sujet sujetCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new EntreeOpa.Sujet(null, List.of());
        }
        String id = auth.getName();
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority autorite : auth.getAuthorities()) {
            String valeur = autorite.getAuthority();
            // On retire le préfixe ROLE_ pour retrouver le nom de rôle attendu par OPA.
            roles.add(valeur.startsWith(PREFIXE_ROLE) ? valeur.substring(PREFIXE_ROLE.length()) : valeur);
        }
        return new EntreeOpa.Sujet(id, roles);
    }
}
