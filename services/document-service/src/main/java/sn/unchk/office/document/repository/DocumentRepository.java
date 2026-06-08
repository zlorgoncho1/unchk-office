package sn.unchk.office.document.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.unchk.office.document.domain.CategorieDocument;
import sn.unchk.office.document.domain.Document;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux métadonnées des documents (non supprimés).
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /** Récupère un document actif (non supprimé logiquement) par son identifiant. */
    Optional<Document> findByIdAndDeletedAtIsNull(UUID id);

    /** Liste paginée des documents actifs. */
    Page<Document> findByDeletedAtIsNull(Pageable pageable);

    /** Liste paginée des documents actifs d'une catégorie donnée. */
    Page<Document> findByCategoryAndDeletedAtIsNull(CategorieDocument category, Pageable pageable);

    /** Vérifie l'unicité du couple (bucket, clé objet) avant insertion. */
    boolean existsByBucketAndObjectKey(String bucket, String objectKey);

    /**
     * Recherche plein-titre simple (insensible à la casse) parmi les documents actifs.
     */
    @Query("""
            select d from Document d
            where d.deletedAt is null
              and lower(d.title) like lower(concat('%', :terme, '%'))
            """)
    Page<Document> rechercherParTitre(@Param("terme") String terme, Pageable pageable);
}
