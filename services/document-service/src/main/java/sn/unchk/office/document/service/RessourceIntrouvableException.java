package sn.unchk.office.document.service;

/**
 * Levée lorsqu'un document demandé n'existe pas (ou est supprimé).
 * <p>
 * Traduite en 404. C'est aussi la réponse retournée en cas de refus OPA en lecture
 * (anti-énumération) : 404 et 403-refusé doivent rester indistincts pour ne pas confirmer
 * l'existence d'un UUID.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
