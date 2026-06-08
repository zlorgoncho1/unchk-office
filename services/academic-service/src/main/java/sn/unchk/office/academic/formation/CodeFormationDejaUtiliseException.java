package sn.unchk.office.academic.formation;

/**
 * Levée lorsqu'on tente d'utiliser un code de formation déjà attribué.
 * <p>
 * Traduite en HTTP 409 (conflit) par l'advice du service.
 */
public class CodeFormationDejaUtiliseException extends RuntimeException {

    public CodeFormationDejaUtiliseException(String code) {
        super("Code de formation déjà utilisé : " + code);
    }
}
