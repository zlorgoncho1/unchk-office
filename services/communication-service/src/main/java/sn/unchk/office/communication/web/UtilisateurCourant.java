package sn.unchk.office.communication.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Résolution de l'identifiant de l'utilisateur courant à partir du contexte de sécurité.
 * <p>
 * Le nom du principal est le claim {@code sub} du JWT (un UUID utilisateur d'identity-service).
 * On le résout TOUJOURS côté serveur : aucun identifiant d'utilisateur n'est jamais accepté
 * depuis le corps ou l'URL de la requête (anti-IDOR, en particulier pour le rôle étudiant).
 */
public final class UtilisateurCourant {

    private UtilisateurCourant() {
        // Classe utilitaire.
    }

    /**
     * @return l'UUID de l'utilisateur authentifié
     * @throws IllegalStateException si aucune identité n'est présente
     */
    public static UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié dans le contexte.");
        }
        return UUID.fromString(auth.getName());
    }
}
