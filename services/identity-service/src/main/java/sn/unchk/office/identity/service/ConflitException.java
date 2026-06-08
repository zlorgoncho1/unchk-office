package sn.unchk.office.identity.service;

/**
 * Conflit de ressource (ex : courriel déjà utilisé). Traduite en HTTP 409.
 */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
