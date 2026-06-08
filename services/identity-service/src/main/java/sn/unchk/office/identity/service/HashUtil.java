package sn.unchk.office.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Utilitaire de hachage SHA-256 pour les refresh tokens.
 * <p>
 * On ne stocke jamais le refresh token brut : seule son empreinte SHA-256 est persistée,
 * de sorte qu'une fuite de la base ne permette pas de rejouer les jetons.
 */
public final class HashUtil {

    private HashUtil() {
        // Classe utilitaire.
    }

    /** Renvoie l'empreinte SHA-256 (hex) d'une valeur. */
    public static String sha256(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] empreinte = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(empreinte);
        } catch (Exception ex) {
            throw new IllegalStateException("Échec du hachage SHA-256", ex);
        }
    }
}
