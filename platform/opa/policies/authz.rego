package unchk.authz

import rego.v1

# Décision d'autorisation centralisée (PDP).
# Le gateway envoie le RBAC grossier (rôle × route) ; les services envoient
# l'ABAC fin (accès au niveau objet) pour prévenir les IDOR.
#
# Entrée attendue :
# {
#   "subject":  {"id": "u-123", "roles": ["enseignant"]},
#   "action":   "read" | "create" | "update" | "delete",
#   "resource": {"type": "document", "id": "d-1", "ownerId": "u-9", "visibility": ["enseignant","admin"]},
#   "request":  {"method": "GET", "path": "/api/documents/d-1"}
# }

default allow := false

# L'administrateur a tous les droits.
allow if "admin" in input.subject.roles

# RBAC grossier : la route + méthode est permise pour au moins un rôle du sujet.
allow if route_allowed

# ABAC / anti-IDOR : lecture autorisée seulement si l'objet est visible par le sujet.
allow if {
	input.action == "read"
	object_visible
}

# Visibilité par rôle déclarée sur la ressource.
object_visible if {
	some r in input.subject.roles
	r in input.resource.visibility
}

# Le propriétaire accède toujours à sa propre ressource.
object_visible if input.resource.ownerId == input.subject.id

route_allowed if {
	some r in input.subject.roles
	some p in data.role_permissions[r]
	method_match(p.method, input.request.method)
	glob.match(p.path, ["/"], input.request.path)
}

method_match(allowed, _) if allowed == "*"
method_match(allowed, m) if allowed == m
