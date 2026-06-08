package sn.unchk.office.people.service;

/**
 * Levee en cas de conflit de donnees (ex : INE ou matricule deja utilise).
 * <p>
 * Traduite en HTTP 409 par le gestionnaire d'exceptions du service.
 */
public class ConflitDonneesException extends RuntimeException {

    public ConflitDonneesException(String message) {
        super(message);
    }
}
