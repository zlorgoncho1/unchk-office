package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.projection.PeopleStaffRo;

import java.util.UUID;

/**
 * Accès au read-model du personnel (projection {@code people.staff}).
 */
public interface PeopleStaffRoRepository extends JpaRepository<PeopleStaffRo, UUID> {
}
