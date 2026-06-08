package sn.unchk.office.insertion.web;

/**
 * Levée quand une ressource demandée n'existe pas (ou n'est plus accessible).
 * <p>
 * Traduite en 404. Anti-IDOR : on répond de manière indistincte d'un accès refusé pour ne
 * pas confirmer l'existence d'un UUID que l'appelant n'aurait pas le droit de consulter.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
