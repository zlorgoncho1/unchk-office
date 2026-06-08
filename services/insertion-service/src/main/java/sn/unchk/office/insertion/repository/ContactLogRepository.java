package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.domain.ContactLog;

import java.util.List;
import java.util.UUID;

/**
 * Accès au registre de contact (suivi des diplômés).
 */
public interface ContactLogRepository extends JpaRepository<ContactLog, UUID> {

    /** Historique des contacts d'un étudiant, du plus récent au plus ancien. */
    List<ContactLog> findByStudentRefOrderByContactedAtDesc(UUID studentRef);
}
