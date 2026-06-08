package sn.unchk.office.common.authz;

/**
 * Point d'extension (SPI) permettant à chaque service de fournir les attributs ABAC
 * d'une ressource (propriétaire, visibilité) à partir de son type et de son identifiant.
 * <p>
 * Le {@link ResourceAccessGuard} appelle cette interface pour enrichir la ressource
 * envoyée à OPA. Sans implémentation, la garde envoie une ressource « minimale »
 * (type + id) et la décision repose alors sur la visibilité par rôle / propriétaire
 * telle que connue d'OPA. Chaque microservice fournit son propre bean d'implémentation
 * (ex : lecture du read-model local) pour un contrôle anti-IDOR précis.
 */
public interface FournisseurAttributsRessource {

    /**
     * Construit les attributs ABAC de la ressource ciblée.
     *
     * @param type type logique de la ressource (document, formation, ...)
     * @param id   identifiant (UUID) de la ressource
     * @return ressource enrichie (ownerId, visibility) prête pour OPA ; jamais {@code null}
     */
    EntreeOpa.Ressource attributs(String type, String id);
}
