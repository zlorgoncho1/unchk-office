package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.Reunion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux réunions (agrégat maître).
 */
public interface ReunionRepository extends JpaRepository<Reunion, UUID> {

    /** Réunions non supprimées, triées par date de début décroissante. */
    List<Reunion> findByDeletedAtIsNullOrderByStartsAtDesc();

    /** Réunion par identifiant, en excluant les supprimées logiquement. */
    Optional<Reunion> findByIdAndDeletedAtIsNull(UUID id);
}
