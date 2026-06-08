package sn.unchk.office.common.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client d'autorisation interrogeant OPA (Policy Decision Point).
 * <p>
 * Appelle {@code POST {OPA_URL}/v1/data/unchk/authz/allow} avec l'enveloppe
 * {@code {"input": {subject, action, resource, request}}} et renvoie la décision.
 * <p>
 * On utilise {@link RestClient} (client synchrone/servlet) car cette librairie cible
 * les services métier MVC, pas le gateway réactif. Le client est volontairement défensif :
 * en cas d'erreur de communication, il refuse (fail-closed / deny-by-default).
 */
public class OpaClient {

    private static final Logger log = LoggerFactory.getLogger(OpaClient.class);

    private final RestClient restClient;
    private final OpaProprietes proprietes;

    /**
     * @param restClient client HTTP préconfiguré avec l'URL de base d'OPA
     * @param proprietes propriétés (chemin de la règle, timeout)
     */
    public OpaClient(RestClient restClient, OpaProprietes proprietes) {
        this.restClient = restClient;
        this.proprietes = proprietes;
    }

    /**
     * Demande à OPA si l'entrée donnée est autorisée.
     *
     * @param entree input complet (sujet × action × ressource × requête)
     * @return {@code true} si OPA autorise, {@code false} sinon ou en cas d'erreur réseau
     */
    public boolean estAutorise(EntreeOpa entree) {
        try {
            ReponseOpa reponse = restClient.post()
                    .uri(proprietes.chemin())
                    .body(entree.versEnveloppe())
                    .retrieve()
                    .body(ReponseOpa.class);

            boolean autorise = reponse != null && reponse.estAutorise();
            if (log.isDebugEnabled()) {
                // Trace de décision sans exposer d'informations sensibles côté client.
                log.debug("Décision OPA action={} typeRessource={} -> {}",
                        entree.action(),
                        entree.resource() != null ? entree.resource().type() : "n/a",
                        autorise ? "AUTORISÉ" : "REFUSÉ");
            }
            return autorise;
        } catch (RestClientException ex) {
            // Indisponibilité d'OPA : on refuse par sécurité (fail-closed).
            log.error("OPA injoignable ou réponse invalide : refus par défaut", ex);
            return false;
        }
    }
}
