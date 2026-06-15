package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.domain.AdminCommunique;
import sn.unchk.office.admin.domain.AdminDocKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux communiqués administratifs (notes de service & circulaires).
 * Le filtre {@code deletedAtIsNull} masque les communiqués supprimés logiquement.
 */
public interface AdminCommuniqueRepository extends JpaRepository<AdminCommunique, UUID> {

    /** Liste les communiqués actifs (les plus récents d'abord). */
    List<AdminCommunique> findByDeletedAtIsNullOrderByIssueDateDesc();

    /** Liste les communiqués actifs d'une nature donnée (note de service / circulaire). */
    List<AdminCommunique> findByKindAndDeletedAtIsNullOrderByIssueDateDesc(AdminDocKind kind);

    /** Charge un communiqué actif (non supprimé) par identifiant. */
    Optional<AdminCommunique> findByIdAndDeletedAtIsNull(UUID id);

    /** Vérifie l'unicité d'une référence avant création. */
    boolean existsByReference(String reference);
}
