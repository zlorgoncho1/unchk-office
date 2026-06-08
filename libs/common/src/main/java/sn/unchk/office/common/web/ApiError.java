package sn.unchk.office.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Structure d'erreur standard renvoyée par l'API, conçue pour ne RIEN divulguer d'interne.
 * <p>
 * Volontairement sobre : ni trace de pile, ni message d'exception brut, ni nom de classe.
 * On expose seulement un statut, un libellé clair, le chemin appelé, l'horodatage et un
 * identifiant de corrélation permettant de relier l'erreur aux journaux côté serveur.
 *
 * @param horodatage    instant de production de l'erreur
 * @param statut        code HTTP (ex : 404)
 * @param erreur        libellé court du statut (ex : "Not Found")
 * @param message       message lisible et sans fuite à destination du client
 * @param chemin        chemin de la requête fautive
 * @param correlationId identifiant de corrélation (X-Correlation-Id) pour le diagnostic
 * @param details       éventuelles erreurs de validation champ par champ (sans donnée sensible)
 */
public record ApiError(
        Instant horodatage,
        int statut,
        String erreur,
        String message,
        String chemin,
        String correlationId,
        List<ErreurChamp> details
) {

    /**
     * Détail d'une erreur de validation sur un champ précis.
     *
     * @param champ   nom du champ en faute
     * @param message raison du rejet (libellé de validation)
     */
    public record ErreurChamp(String champ, String message) {
    }

    /**
     * Fabrique une erreur simple (sans détails de validation).
     */
    public static ApiError de(int statut, String erreur, String message,
                              String chemin, String correlationId) {
        return new ApiError(Instant.now(), statut, erreur, message, chemin, correlationId, List.of());
    }

    /**
     * Fabrique une erreur de validation avec le détail des champs en faute.
     */
    public static ApiError deValidation(int statut, String erreur, String message,
                                        String chemin, String correlationId,
                                        List<ErreurChamp> details) {
        return new ApiError(Instant.now(), statut, erreur, message, chemin, correlationId, details);
    }
}
