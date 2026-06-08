package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.projection.PeopleStudentRo;

import java.util.UUID;

/**
 * Accès au read-model local des étudiants (projection de people.students).
 * Écrit uniquement par le consommateur Kafka, lu pour valider les références et les stats.
 */
public interface PeopleStudentRoRepository extends JpaRepository<PeopleStudentRo, UUID> {
}
