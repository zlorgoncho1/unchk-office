package sn.unchk.office.identity.dto;

import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Représentation publique d'un compte utilisateur (jamais le hash de mot de passe).
 *
 * @param id           identifiant du compte
 * @param email        courriel
 * @param fullName     nom complet
 * @param roles        rôles accordés
 * @param active       compte activé
 * @param locked       compte verrouillé
 * @param personRef    référence vers la personne canonique (optionnel)
 * @param personKind   nature de la personne liée (optionnel)
 * @param lastLoginAt  dernière connexion réussie (optionnel)
 * @param createdAt    date de création
 */
public record VueUtilisateur(
        UUID id,
        String email,
        String fullName,
        List<String> roles,
        boolean active,
        boolean locked,
        UUID personRef,
        String personKind,
        Instant lastLoginAt,
        Instant createdAt
) {

    /** Construit la vue à partir de l'entité et de ses rôles (sans exposer de secret). */
    public static VueUtilisateur depuis(Utilisateur u, List<RoleUtilisateur> roles) {
        List<String> libelles = roles.stream()
                .map(r -> r.getRole().libelle())
                .toList();
        return new VueUtilisateur(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                libelles,
                u.isActive(),
                u.isLocked(),
                u.getPersonRef(),
                u.getPersonKind(),
                u.getLastLoginAt(),
                u.getCreatedAt());
    }
}
