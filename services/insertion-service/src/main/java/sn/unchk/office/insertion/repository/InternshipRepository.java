package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.domain.Internship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux stages (bilans de stages). Filtre les stages supprimés logiquement.
 */
public interface InternshipRepository extends JpaRepository<Internship, UUID> {

    Optional<Internship> findByIdAndDeletedAtIsNull(UUID id);

    /** Stages d'un étudiant donné (suivi de son propre parcours). */
    List<Internship> findByStudentRefAndDeletedAtIsNull(UUID studentRef);

    List<Internship> findByDeletedAtIsNull();
}
