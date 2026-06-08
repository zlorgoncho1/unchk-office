package sn.unchk.office.academic.emploidutemps;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux créneaux d'emploi du temps.
 */
public interface CreneauRepository extends JpaRepository<Creneau, UUID> {

    /** Liste les créneaux d'une formation. */
    List<Creneau> findByFormationId(UUID formationId);

    /** Liste les créneaux affectés à un formateur donné. */
    List<Creneau> findByFormateurRef(UUID formateurRef);
}
