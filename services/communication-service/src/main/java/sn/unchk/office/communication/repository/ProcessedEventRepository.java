package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.ProcessedEvent;

import java.util.UUID;

/**
 * Trace des événements Kafka déjà consommés (idempotence).
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
