package sn.unchk.office.document.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.document.domain.OutboxMessage;

import java.util.List;
import java.util.UUID;

/**
 * Accès à la file Outbox (messages en attente de publication vers Kafka).
 */
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    /** Messages non encore publiés, du plus ancien au plus récent. */
    List<OutboxMessage> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
