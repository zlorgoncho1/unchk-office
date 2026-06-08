package sn.unchk.office.identity.securite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de durcissement de l'authentification.
 * <p>
 * Renseignées sous le préfixe {@code unchk.identity.securite}.
 *
 * @param maxEchecs nombre d'échecs de connexion tolérés avant verrouillage du compte (anti-bruteforce)
 */
@ConfigurationProperties(prefix = "unchk.identity.securite")
public record SecuriteProprietes(Integer maxEchecs) {

    public SecuriteProprietes {
        if (maxEchecs == null || maxEchecs <= 0) {
            maxEchecs = 5;
        }
    }
}
