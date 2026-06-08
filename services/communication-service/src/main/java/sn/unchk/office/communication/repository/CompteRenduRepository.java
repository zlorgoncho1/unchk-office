package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.CompteRendu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux comptes rendus (agrégat maître).
 */
public interface CompteRenduRepository extends JpaRepository<CompteRendu, UUID> {

    /** Comptes rendus non supprimés, du plus récent au plus ancien. */
    List<CompteRendu> findByDeletedAtIsNullOrderByMeetingDateDesc();

    /** Compte rendu par identifiant, en excluant les supprimés logiquement. */
    Optional<CompteRendu> findByIdAndDeletedAtIsNull(UUID id);
}
