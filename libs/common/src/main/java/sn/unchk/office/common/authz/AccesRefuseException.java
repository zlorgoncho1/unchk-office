package sn.unchk.office.common.authz;

/**
 * Levée lorsqu'OPA refuse l'accès au niveau objet (protection anti-IDOR).
 * <p>
 * Le {@link sn.unchk.office.common.web.GlobalExceptionHandler} la traduit en réponse
 * HTTP 403 sans divulguer d'information sur la ressource (anti-énumération).
 */
public class AccesRefuseException extends RuntimeException {

    public AccesRefuseException(String message) {
        super(message);
    }
}
