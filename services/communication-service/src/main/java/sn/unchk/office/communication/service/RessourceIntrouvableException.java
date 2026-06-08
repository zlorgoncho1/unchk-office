package sn.unchk.office.communication.service;

/**
 * Levée quand une ressource demandée n'existe pas (ou est supprimée logiquement).
 * <p>
 * Traduite en HTTP 404 par le contrôleur. Sert aussi l'anti-énumération : un accès refusé
 * en lecture renvoie 404 (et non 403) pour ne pas confirmer l'existence d'un UUID.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
