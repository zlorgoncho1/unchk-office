package sn.unchk.office.document.storage;

/**
 * Erreur technique survenue lors d'un échange avec le stockage objet MinIO.
 * <p>
 * Traduite en 500 sobre par le gestionnaire d'erreurs global (sans fuite d'interne).
 */
public class StockageException extends RuntimeException {

    public StockageException(String message, Throwable cause) {
        super(message, cause);
    }
}
