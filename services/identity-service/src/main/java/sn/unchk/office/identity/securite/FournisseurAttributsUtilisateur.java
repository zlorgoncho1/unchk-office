package sn.unchk.office.identity.securite;

import org.springframework.stereotype.Component;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;
import sn.unchk.office.identity.depot.UtilisateurRepository;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.util.List;
import java.util.UUID;

/**
 * Fournisseur d'attributs ABAC pour les ressources de type {@code user} (anti-IDOR).
 * <p>
 * Charge le compte ciblé depuis la base et renseigne son propriétaire ({@code ownerId} =
 * l'utilisateur lui-même). OPA peut alors autoriser le propriétaire à accéder à son propre
 * compte (et l'admin via la règle globale), tout en refusant l'accès aux comptes d'autrui.
 * Lecture seule, aucun appel REST inter-service.
 */
@Component
public class FournisseurAttributsUtilisateur implements FournisseurAttributsRessource {

    private static final String TYPE_USER = "user";

    private final UtilisateurRepository depot;

    public FournisseurAttributsUtilisateur(UtilisateurRepository depot) {
        this.depot = depot;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        if (!TYPE_USER.equals(type)) {
            // Type non géré ici : ressource minimale, OPA tranche selon ses données.
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        UUID uuid = parser(id);
        String ownerId = null;
        if (uuid != null) {
            // Le propriétaire d'un compte est le compte lui-même.
            Utilisateur utilisateur = depot.findById(uuid).orElse(null);
            if (utilisateur != null) {
                ownerId = utilisateur.getId().toString();
            }
        }
        // Pas de visibilité par rôle sur un compte : seul le propriétaire (ou l'admin) y accède.
        return new EntreeOpa.Ressource(TYPE_USER, id, ownerId, List.of());
    }

    private UUID parser(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
