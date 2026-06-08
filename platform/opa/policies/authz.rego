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

# Notifications personnelles : tout utilisateur authentifié consulte et marque
# les SIENNES (le service filtre sur l'identité de l'appelant ; OPA n'autorise
# que la route, indépendamment du rôle).
allow if {
	input.request.method == "GET"
	startswith(input.request.path, "/api/communication/notifications")
}

allow if {
	input.request.method == "PATCH"
	startswith(input.request.path, "/api/communication/notifications/")
}

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

# ------------------------------------------------------------------
# Décision d'accès au niveau OBJET (ABAC strict, anti-IDOR).
# Interrogée par les services (ResourceAccessGuard / @VerifieAccesObjet) sur l'accès à une
# ressource PRÉCISE par son id. Contrairement à `allow`, elle N'accorde PAS l'accès sur la
# seule base du RBAC de route : disposer de `GET /api/**` ne suffit pas a lire un objet.
# Il faut être admin, propriétaire de l'objet, ou que l'objet soit explicitement visible
# par l'un des rôles du sujet (visibility déclarée en base). Empêche les IDOR où un rôle au
# large droit de lecture lirait un objet hors de sa visibilité.
default allow_objet := false

allow_objet if "admin" in input.subject.roles

allow_objet if input.resource.ownerId == input.subject.id

allow_objet if object_visible

# Domaine « insertion » (suivi du devenir, registre de contact, situations) : les rôles de
# gestion de l'insertion accèdent aux fiches de TOUS les étudiants suivis — c'est leur
# périmètre métier (déclaré côté service par INSERTION_ROLES). Reste cantonné aux types
# insertion : ne réouvre pas l'IDOR sur documents/comptes rendus.
allow_objet if {
	input.resource.type in {"insertion", "outcome"}
	some r in input.subject.roles
	r in {"administratif", "appui-insertion"}
}
