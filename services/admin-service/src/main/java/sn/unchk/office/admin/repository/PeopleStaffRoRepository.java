package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.projection.PeopleStaffRo;

import java.util.UUID;

/**
 * Accès au read-model du personnel (projection Kafka). Lecture seule côté API.
 */
public interface PeopleStaffRoRepository extends JpaRepository<PeopleStaffRo, UUID> {
}
