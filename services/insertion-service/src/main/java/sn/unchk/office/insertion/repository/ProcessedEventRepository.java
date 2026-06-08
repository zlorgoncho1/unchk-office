package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.messaging.ProcessedEvent;

import java.util.UUID;

/**
 * Journal d'idempotence des événements Kafka consommés.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
