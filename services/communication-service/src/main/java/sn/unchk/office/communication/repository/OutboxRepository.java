package sn.unchk.office.communication.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.OutboxMessage;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux messages Outbox (Transactional Outbox).
 */
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    /** Messages non encore relayés vers Kafka, du plus ancien au plus récent. */
    List<OutboxMessage> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
