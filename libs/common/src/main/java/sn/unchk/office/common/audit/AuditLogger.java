package sn.unchk.office.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;

import java.time.Instant;

/**
 * Journalisation d'audit structurée : trace « qui a fait quoi, sur quoi, avec quel résultat ».
 * <p>
 * Émet une ligne de log dédiée (logger {@code AUDIT}) avec des champs stables, exploitables
 * par un agrégateur de logs. L'acteur est déduit du contexte de sécurité courant ; le champ
 * de corrélation est repris du MDC (posé par le filtre de corrélation) pour relier l'action
 * à la requête HTTP d'origine.
 */
public class AuditLogger {

    /** Logger dédié à l'audit, séparable du reste par configuration de log. */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /** Clé MDC de corrélation, alignée avec le filtre web. */
    private static final String CLE_CORRELATION = "correlationId";

    /**
     * Journalise une action réussie.
     *
     * @param action      action effectuée (ex : "CREATION_DOCUMENT")
     * @param typeObjet   type de l'objet visé (ex : "document")
     * @param idObjet     identifiant de l'objet visé (UUID)
     */
    public void succes(String action, String typeObjet, String idObjet) {
        ecrire(action, typeObjet, idObjet, "SUCCES", null);
    }

    /**
     * Journalise une action refusée ou échouée.
     *
     * @param action    action tentée
     * @param typeObjet type de l'objet visé
     * @param idObjet   identifiant de l'objet visé
     * @param motif     raison de l'échec / du refus (sans donnée sensible)
     */
    public void echec(String action, String typeObjet, String idObjet, String motif) {
        ecrire(action, typeObjet, idObjet, "ECHEC", motif);
    }

    /**
     * Construit et émet la ligne d'audit structurée.
     */
    private void ecrire(String action, String typeObjet, String idObjet,
                        String resultat, String motif) {
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        String acteur = sujet.id() != null ? sujet.id() : "anonyme";
        String correlationId = MDC.get(CLE_CORRELATION);

        // Format clé=valeur : lisible humainement et facile à parser par un agrégateur.
        audit.info("audit horodatage={} acteur={} roles={} action={} typeObjet={} idObjet={} resultat={} motif={} correlationId={}",
                Instant.now(),
                acteur,
                sujet.roles(),
                action,
                typeObjet,
                idObjet,
                resultat,
                motif != null ? motif : "-",
                correlationId != null ? correlationId : "-");
    }
}
