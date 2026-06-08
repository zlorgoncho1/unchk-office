package sn.unchk.office.document.authz;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;
import sn.unchk.office.document.domain.Document;
import sn.unchk.office.document.repository.DocumentRepository;
import sn.unchk.office.document.repository.DocumentVisibilityRepository;

import java.util.List;
import java.util.UUID;

/**
 * Fournit à OPA les attributs ABAC d'un document (propriétaire + visibilité par rôle),
 * lus depuis la base locale. C'est le rempart anti-IDOR : la garde
 * {@link sn.unchk.office.common.authz.ResourceAccessGuard} appelle ce fournisseur avant de
 * laisser l'accès à un document désigné par son UUID.
 * <p>
 * Si le document n'existe pas (ou est supprimé), on renvoie une ressource « vide » :
 * OPA refusera (deny-by-default) et le contrôleur renverra 404 (anti-énumération).
 */
@Component
public class FournisseurAttributsDocument implements FournisseurAttributsRessource {

    /** Type logique géré par ce fournisseur (aligné avec l'annotation @VerifieAccesObjet). */
    private static final String TYPE_DOCUMENT = "document";

    private final DocumentRepository documents;
    private final DocumentVisibilityRepository visibilites;

    public FournisseurAttributsDocument(DocumentRepository documents,
                                        DocumentVisibilityRepository visibilites) {
        this.documents = documents;
        this.visibilites = visibilites;
    }

    @Override
    @Transactional(readOnly = true)
    public EntreeOpa.Ressource attributs(String type, String id) {
        // On ne traite que le type « document » ; sinon ressource minimale.
        if (!TYPE_DOCUMENT.equals(type)) {
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        UUID documentId;
        try {
            documentId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            // Identifiant non UUID : ressource vide -> OPA refuse.
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        return documents.findByIdAndDeletedAtIsNull(documentId)
                .map(this::versRessource)
                // Document inconnu : on n'expose ni owner ni visibilité (OPA refuse, 404 ensuite).
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    /** Construit la ressource OPA enrichie (ownerId + rôles visibles). */
    private EntreeOpa.Ressource versRessource(Document document) {
        List<String> roles = visibilites.rolesAutorises(document.getId());
        String ownerId = document.getOwnerId() != null ? document.getOwnerId().toString() : null;
        return new EntreeOpa.Ressource(TYPE_DOCUMENT, document.getId().toString(), ownerId, roles);
    }
}
