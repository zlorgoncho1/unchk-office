package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.projection.ProcessedEvent;

import java.util.UUID;

/**
 * Accès aux événements déjà traités (idempotence des consommateurs Kafka).
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
