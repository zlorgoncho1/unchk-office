package sn.unchk.office.academic.config;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Accès aux événements déjà traités (idempotence des consommateurs Kafka).
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
