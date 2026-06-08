package sn.unchk.office.identity.service;

/**
 * Ressource (compte) introuvable. Traduite en HTTP 404.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
