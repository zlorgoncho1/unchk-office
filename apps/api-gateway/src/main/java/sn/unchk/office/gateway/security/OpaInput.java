package sn.unchk.office.gateway.security;

import java.util.List;

/**
 * Modèle de la requête envoyée à OPA pour la décision d'autorisation.
 *
 * <p>Format aligné sur la politique Rego {@code unchk.authz} :</p>
 * <pre>
 * {
 *   "subject":  {"id": "u-123", "roles": ["enseignant"]},
 *   "action":   "read" | "create" | "update" | "delete",
 *   "resource": {"type": "documents", ...},
 *   "request":  {"method": "GET", "path": "/api/documents/d-1"}
 * }
 * </pre>
 *
 * <p>Au niveau de la passerelle, seul le RBAC grossier (rôle × route) est évalué :
 * on renseigne le sujet, l'action (déduite de la méthode HTTP) et la requête.
 * L'ABAC fin (accès au niveau objet, anti-IDOR) est porté par chaque service via libs/common.</p>
 */
public record OpaInput(Subject subject, String action, Resource resource, Request request) {

    /** Le PDP attend la structure {"input": {...}} en entrée. */
    public record Body(OpaInput input) {
    }

    /** Sujet authentifié : identifiant UUID + rôles issus du JWT. */
    public record Subject(String id, List<String> roles) {
    }

    /** Ressource ciblée : au niveau passerelle on ne connaît que son type (segment de chemin). */
    public record Resource(String type) {
    }

    /** Détails de la requête HTTP entrante. */
    public record Request(String method, String path) {
    }
}
