package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.identity.domaine.ReadPerson;

import java.util.UUID;

/**
 * Accès au read-model local des personnes canoniques (projection people.*).
 */
public interface ReadPersonRepository extends JpaRepository<ReadPerson, UUID> {
}
