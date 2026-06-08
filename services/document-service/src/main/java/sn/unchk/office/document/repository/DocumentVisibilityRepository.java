package sn.unchk.office.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.unchk.office.document.domain.DocumentVisibility;

import java.util.List;
import java.util.UUID;

/**
 * Accès à la visibilité par rôle des documents (alimente le visibility[] OPA).
 */
public interface DocumentVisibilityRepository
        extends JpaRepository<DocumentVisibility, DocumentVisibility.Cle> {

    /** Tous les rôles autorisés à voir un document. */
    @Query("select v.cle.role from DocumentVisibility v where v.cle.documentId = :documentId")
    List<String> rolesAutorises(@Param("documentId") UUID documentId);

    /** Toutes les lignes de visibilité d'un document. */
    List<DocumentVisibility> findByCleDocumentId(UUID documentId);

    /** Supprime la visibilité d'un document (avant ré-écriture). */
    void deleteByCleDocumentId(UUID documentId);
}
