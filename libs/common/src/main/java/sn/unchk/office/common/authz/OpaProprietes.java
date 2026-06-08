package sn.unchk.office.common.authz;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du client OPA.
 * <p>
 * Renseignées sous le préfixe {@code unchk.authz.opa} dans {@code application.yml}.
 *
 * @param url     URL de base du serveur OPA (ex : {@code http://opa:8181})
 * @param chemin  chemin de la règle d'autorisation sur l'API Data
 *                (par défaut {@code /v1/data/unchk/authz/allow})
 * @param timeout délai maximal (en millisecondes) d'attente de la réponse OPA
 */
@ConfigurationProperties(prefix = "unchk.authz.opa")
public record OpaProprietes(
        String url,
        String chemin,
        Long timeout
) {

    /** Chemin par défaut de la règle {@code allow} dans le paquet {@code unchk.authz}. */
    public static final String CHEMIN_PAR_DEFAUT = "/v1/data/unchk/authz/allow";

    /** Délai par défaut (2 secondes) : OPA est local, la décision doit être quasi immédiate. */
    public static final long TIMEOUT_PAR_DEFAUT_MS = 2000L;

    public OpaProprietes {
        // Valeurs de repli si l'opérateur ne précise pas tout dans la configuration.
        if (chemin == null || chemin.isBlank()) {
            chemin = CHEMIN_PAR_DEFAUT;
        }
        if (timeout == null || timeout <= 0) {
            timeout = TIMEOUT_PAR_DEFAUT_MS;
        }
    }
}
