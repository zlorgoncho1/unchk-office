package sn.unchk.office.admin.service;

/**
 * Levée lorsqu'une règle d'unicité métier est violée (ex : budget déjà existant pour
 * un couple exercice + libellé). Traduite en réponse HTTP 409 par le contrôleur d'erreurs local.
 */
public class ConflitRessourceException extends RuntimeException {

    public ConflitRessourceException(String message) {
        super(message);
    }
}
