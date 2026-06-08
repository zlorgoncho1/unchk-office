package sn.unchk.office.identity.securite;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Conversion entre clés RSA Java et leur représentation PEM (texte stocké en base).
 * <p>
 * Le format PEM permet de persister les clés dans les colonnes {@code public_pem} /
 * {@code private_pem} de la table {@code signing_keys}, puis de les recharger au démarrage.
 */
public final class PemUtil {

    private static final String DEBUT_PUBLIQUE = "-----BEGIN PUBLIC KEY-----";
    private static final String FIN_PUBLIQUE = "-----END PUBLIC KEY-----";
    private static final String DEBUT_PRIVEE = "-----BEGIN PRIVATE KEY-----";
    private static final String FIN_PRIVEE = "-----END PRIVATE KEY-----";

    private PemUtil() {
        // Classe utilitaire.
    }

    /** Encode une clé publique RSA (X.509) en PEM. */
    public static String versPem(PublicKey cle) {
        return encadrer(DEBUT_PUBLIQUE, FIN_PUBLIQUE, cle.getEncoded());
    }

    /** Encode une clé privée RSA (PKCS#8) en PEM. */
    public static String versPem(PrivateKey cle) {
        return encadrer(DEBUT_PRIVEE, FIN_PRIVEE, cle.getEncoded());
    }

    /** Reconstruit une clé publique RSA depuis son PEM. */
    public static PublicKey clePubliqueDepuisPem(String pem) {
        byte[] octets = decoder(pem, DEBUT_PUBLIQUE, FIN_PUBLIQUE);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(octets));
        } catch (Exception ex) {
            throw new IllegalStateException("Clé publique PEM illisible", ex);
        }
    }

    /** Reconstruit une clé privée RSA depuis son PEM. */
    public static PrivateKey clePriveeDepuisPem(String pem) {
        byte[] octets = decoder(pem, DEBUT_PRIVEE, FIN_PRIVEE);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(octets));
        } catch (Exception ex) {
            throw new IllegalStateException("Clé privée PEM illisible", ex);
        }
    }

    private static String encadrer(String debut, String fin, byte[] contenu) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(contenu);
        return debut + "\n" + base64 + "\n" + fin + "\n";
    }

    private static byte[] decoder(String pem, String debut, String fin) {
        String corps = pem
                .replace(debut, "")
                .replace(fin, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(corps);
    }
}
