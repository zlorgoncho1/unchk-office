package sn.unchk.office.identity.messaging;

import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.util.List;
import java.util.UUID;

/**
 * Charge utile publiée sur le topic {@code identity.users} (état du compte).
 * <p>
 * Event-carried state transfer : transporte l'état courant du compte (identité, rôles, statut)
 * pour que les autres services maintiennent leurs read-models (qui notifier, révocation rapide).
 * Ne contient JAMAIS de hash ni de mot de passe (conformité sécurité / docs).
 *
 * @param userId     identifiant du compte (clé de partition)
 * @param email      courriel
 * @param fullName   nom complet
 * @param roles      rôles (libellés canoniques)
 * @param status     statut : ACTIVE / SUSPENDED / DISABLED
 * @param personRef  référence vers la personne canonique (optionnel)
 * @param personKind nature de la personne liée (optionnel)
 */
public record UtilisateurEvenement(
        UUID userId,
        String email,
        String fullName,
        List<String> roles,
        String status,
        UUID personRef,
        String personKind
) {

    /** Statut ACTIVE : compte utilisable. */
    public static final String STATUT_ACTIF = "ACTIVE";
    /** Statut SUSPENDED : compte verrouillé (anti-bruteforce) ou suspendu. */
    public static final String STATUT_SUSPENDU = "SUSPENDED";
    /** Statut DISABLED : compte désactivé ou supprimé logiquement. */
    public static final String STATUT_DESACTIVE = "DISABLED";

    /**
     * Construit l'évènement d'état à partir de l'entité et de ses rôles.
     * Le statut est déduit des indicateurs d'activation / verrouillage / suppression.
     */
    public static UtilisateurEvenement depuis(Utilisateur u, List<RoleUtilisateur> roles) {
        List<String> libelles = roles.stream()
                .map(r -> r.getRole().libelle())
                .toList();
        return new UtilisateurEvenement(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                libelles,
                statut(u),
                u.getPersonRef(),
                u.getPersonKind());
    }

    private static String statut(Utilisateur u) {
        if (u.getDeletedAt() != null || !u.isActive()) {
            return STATUT_DESACTIVE;
        }
        if (u.isLocked()) {
            return STATUT_SUSPENDU;
        }
        return STATUT_ACTIF;
    }
}
