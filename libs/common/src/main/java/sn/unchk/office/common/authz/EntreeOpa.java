package sn.unchk.office.common.authz;

import java.util.List;
import java.util.Map;

/**
 * Structure d'entrée envoyée à OPA pour une décision d'autorisation.
 * <p>
 * Le format reflète exactement ce qu'attend la politique Rego {@code unchk.authz} :
 * <pre>
 * {
 *   "subject":  {"id": "...", "roles": [...]},
 *   "action":   "read|create|update|delete",
 *   "resource": {"type": "...", "id": "...", "ownerId": "...", "visibility": [...]},
 *   "request":  {"method": "GET", "path": "/api/..."}
 * }
 * </pre>
 *
 * @param subject  identité et rôles de l'appelant
 * @param action   action demandée (read, create, update, delete)
 * @param resource ressource ciblée (pour l'ABAC anti-IDOR au niveau objet)
 * @param request  contexte HTTP (méthode + chemin) pour le RBAC de route
 */
public record EntreeOpa(
        Sujet subject,
        String action,
        Ressource resource,
        Requete request
) {

    /**
     * Sujet de la décision : identifiant de l'utilisateur (claim {@code sub}) et ses rôles.
     *
     * @param id    identifiant de l'utilisateur
     * @param roles rôles portés par l'utilisateur
     */
    public record Sujet(String id, List<String> roles) {
    }

    /**
     * Ressource ciblée par l'action, décrite pour l'ABAC.
     *
     * @param type       type de ressource (document, formation, etudiant, ...)
     * @param id         identifiant (UUID) de la ressource
     * @param ownerId    identifiant du propriétaire (pour la règle « propriétaire »)
     * @param visibility rôles autorisés à voir la ressource (visibilité déclarée)
     */
    public record Ressource(
            String type,
            String id,
            String ownerId,
            List<String> visibility
    ) {
    }

    /**
     * Contexte HTTP de la requête, exploité par le RBAC de route côté gateway.
     *
     * @param method méthode HTTP (GET, POST, ...)
     * @param path   chemin appelé
     */
    public record Requete(String method, String path) {
    }

    /**
     * OPA attend l'enveloppe {@code {"input": ...}} sur l'API Data.
     * Cette méthode produit la map prête à être sérialisée en JSON.
     */
    public Map<String, Object> versEnveloppe() {
        return Map.of("input", this);
    }
}
