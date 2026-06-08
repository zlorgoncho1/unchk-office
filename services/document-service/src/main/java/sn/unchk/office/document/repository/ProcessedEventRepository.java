package sn.unchk.office.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.document.domain.ProcessedEvent;

import java.util.UUID;

/**
 * Accès à la table d'idempotence des consommateurs Kafka.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
