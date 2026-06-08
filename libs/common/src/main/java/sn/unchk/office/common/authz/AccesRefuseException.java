package sn.unchk.office.common.authz;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée lorsqu'OPA refuse l'accès au niveau objet (protection anti-IDOR).
 * <p>
 * Annotée {@link ResponseStatus} {@code FORBIDDEN} : Spring la traduit en HTTP 403 même dans
 * les services qui n'enregistrent pas le {@link sn.unchk.office.common.web.GlobalExceptionHandler}
 * (chacun ayant son propre advice). Le message reste générique pour ne pas révéler l'existence
 * de la ressource (anti-énumération).
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesRefuseException extends RuntimeException {

    public AccesRefuseException(String message) {
        super(message);
    }
}
