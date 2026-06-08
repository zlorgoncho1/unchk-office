package sn.unchk.office.people.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.people.domain.ProcessedEvent;

import java.util.UUID;

/**
 * Registre des evenements Kafka deja traites (idempotence des consommateurs).
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /** Indique si l'evenement a deja ete consomme (et doit donc etre ignore). */
    boolean existsByEventId(UUID eventId);
}
