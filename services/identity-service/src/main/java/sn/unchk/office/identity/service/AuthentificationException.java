package sn.unchk.office.identity.service;

/**
 * Échec d'authentification (identifiants invalides, compte verrouillé/désactivé,
 * refresh token invalide). Traduite en HTTP 401 sans révéler la cause précise
 * (anti-énumération de comptes).
 */
public class AuthentificationException extends RuntimeException {

    public AuthentificationException(String message) {
        super(message);
    }
}
