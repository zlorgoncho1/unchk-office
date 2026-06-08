package sn.unchk.office.academic.formation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux formations (agrégat racine).
 * <p>
 * Les méthodes filtrent systématiquement sur la non-suppression logique
 * ({@code deleted_at IS NULL}) pour ne jamais exposer une formation supprimée.
 */
public interface FormationRepository extends JpaRepository<Formation, UUID> {

    /** Récupère une formation active (non supprimée) par son identifiant. */
    Optional<Formation> findByIdAndDeletedAtIsNull(UUID id);

    /** Liste les formations non supprimées. */
    List<Formation> findByDeletedAtIsNull();

    /** Liste les formations non supprimées pour un niveau donné. */
    List<Formation> findByLevelAndDeletedAtIsNull(NiveauFormation level);

    /** Vérifie l'unicité du code de formation. */
    boolean existsByCode(String code);
}
