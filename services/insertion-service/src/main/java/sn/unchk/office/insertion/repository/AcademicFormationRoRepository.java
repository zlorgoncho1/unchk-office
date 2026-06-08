package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.projection.AcademicFormationRo;

import java.util.UUID;

/**
 * Accès au read-model local des formations (projection de academic.formations).
 * Écrit uniquement par le consommateur Kafka, lu pour étiqueter les statistiques.
 */
public interface AcademicFormationRoRepository extends JpaRepository<AcademicFormationRo, UUID> {
}
