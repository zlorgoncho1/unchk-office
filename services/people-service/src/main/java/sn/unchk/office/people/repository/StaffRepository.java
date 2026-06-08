package sn.unchk.office.people.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.unchk.office.people.domain.Staff;

import java.util.Optional;
import java.util.UUID;

/**
 * Acces au personnel / formateurs canoniques.
 * <p>
 * Les requetes excluent le personnel supprime logiquement ({@code deletedAt IS NULL}).
 */
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    @Query("SELECT s FROM Staff s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Staff> findActifById(@Param("id") UUID id);

    @Query("SELECT s FROM Staff s WHERE s.deletedAt IS NULL")
    Page<Staff> findTousActifs(Pageable pageable);

    boolean existsByMatricule(String matricule);
}
