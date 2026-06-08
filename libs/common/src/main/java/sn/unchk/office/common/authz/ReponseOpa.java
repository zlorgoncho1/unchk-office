package sn.unchk.office.common.authz;

/**
 * Réponse de l'API Data d'OPA.
 * <p>
 * L'appel {@code POST /v1/data/unchk/authz/allow} renvoie un corps de la forme
 * {@code {"result": true|false}}. La valeur {@code result} porte la décision.
 *
 * @param result décision d'autorisation ({@code true} = autorisé). Peut être {@code null}
 *               si la règle n'est pas définie : on traite alors comme un refus (deny-by-default).
 */
public record ReponseOpa(Boolean result) {

    /**
     * Indique si la décision est un accord explicite.
     * Tout autre cas (null, false) est un refus, conformément au deny-by-default.
     */
    public boolean estAutorise() {
        return Boolean.TRUE.equals(result);
    }
}
