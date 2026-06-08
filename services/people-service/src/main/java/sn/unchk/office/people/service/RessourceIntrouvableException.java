package sn.unchk.office.people.service;

/**
 * Levee lorsqu'une ressource demandee n'existe pas (ou est supprimee logiquement).
 * <p>
 * Traduite en HTTP 404 par le gestionnaire d'exceptions du service. Sur une ressource
 * existante mais non autorisee, on renvoie aussi 404 (anti-enumeration / anti-IDOR).
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
