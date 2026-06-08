package sn.unchk.office.communication.security;

import org.springframework.stereotype.Component;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;
import sn.unchk.office.communication.domain.CompteRendu;
import sn.unchk.office.communication.repository.CompteRenduRepository;

import java.util.List;
import java.util.UUID;

/**
 * Fournit à OPA les attributs ABAC réels des ressources de ce service (anti-IDOR).
 * <p>
 * Le {@link sn.unchk.office.common.authz.ResourceAccessGuard} appelle ce bean avant d'autoriser
 * l'accès à un objet. Pour un compte rendu, on charge le {@code ownerId} (créateur) et la
 * {@code visibility} (rôles) réellement enregistrés en base : OPA tranche alors sur des
 * attributs vérifiés, pas sur ce que prétend le client.
 * <p>
 * Si la ressource est introuvable, on renvoie une visibilité vide et aucun propriétaire :
 * OPA refusera (deny-by-default), ce qui se traduit par un 404 (anti-énumération).
 */
@Component
public class FournisseurAttributsCommunication implements FournisseurAttributsRessource {

    /** Type logique des comptes rendus pour l'annotation {@code @VerifieAccesObjet}. */
    public static final String TYPE_COMPTE_RENDU = "compte-rendu";

    private final CompteRenduRepository compteRenduRepository;

    public FournisseurAttributsCommunication(CompteRenduRepository compteRenduRepository) {
        this.compteRenduRepository = compteRenduRepository;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        if (TYPE_COMPTE_RENDU.equals(type)) {
            return attributsCompteRendu(id);
        }
        // Type non géré ici : ressource minimale, OPA décidera selon ses propres données.
        return new EntreeOpa.Ressource(type, id, null, List.of());
    }

    /** Charge le propriétaire et la visibilité d'un compte rendu pour l'ABAC. */
    private EntreeOpa.Ressource attributsCompteRendu(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            // Identifiant non-UUID : ressource vide -> refus -> 404.
            return new EntreeOpa.Ressource(TYPE_COMPTE_RENDU, id, null, List.of());
        }
        return compteRenduRepository.findByIdAndDeletedAtIsNull(uuid)
                .map(this::versRessource)
                .orElseGet(() -> new EntreeOpa.Ressource(TYPE_COMPTE_RENDU, id, null, List.of()));
    }

    private EntreeOpa.Ressource versRessource(CompteRendu cr) {
        String ownerId = cr.getCreatedBy() != null ? cr.getCreatedBy().toString() : null;
        return new EntreeOpa.Ressource(
                TYPE_COMPTE_RENDU,
                cr.getId().toString(),
                ownerId,
                List.copyOf(cr.getVisibility()));
    }
}
