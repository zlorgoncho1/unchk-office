package sn.unchk.office.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.document.domain.DocumentShare;

import java.util.List;
import java.util.UUID;

/**
 * Accès au partage nominatif des documents (par utilisateur).
 */
public interface DocumentShareRepository
        extends JpaRepository<DocumentShare, DocumentShare.Cle> {

    /** Tous les partages d'un document. */
    List<DocumentShare> findByCleDocumentId(UUID documentId);

    /** Vrai si le document est partagé avec cet utilisateur. */
    boolean existsByCleDocumentIdAndCleUserId(UUID documentId, UUID userId);
}
