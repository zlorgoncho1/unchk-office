package sn.unchk.office.admin.service;

/**
 * Levée lorsqu'une ressource demandée n'existe pas (ou n'est pas visible).
 * <p>
 * Traduite en réponse HTTP 404 par le contrôleur. Le message reste générique pour ne pas
 * confirmer l'existence d'un UUID (anti-énumération).
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
