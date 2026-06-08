package sn.unchk.office.gateway.security;

/**
 * Réponse d'OPA sur le chemin de décision {@code /v1/data/unchk/authz/allow}.
 *
 * <p>OPA renvoie {@code {"result": true|false}}. En cas d'absence de résultat
 * (politique introuvable), {@code result} est {@code null} et l'on refuse par défaut.</p>
 */
public record OpaResult(Boolean result) {

    /** Vrai uniquement si OPA a explicitement autorisé (deny-by-default sinon). */
    public boolean allowed() {
        return Boolean.TRUE.equals(result);
    }
}
