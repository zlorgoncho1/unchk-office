package sn.unchk.office.people.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Marqueur d'idempotence des consommateurs Kafka.
 * <p>
 * Chaque {@code eventId} traite est enregistre une seule fois : si le meme evenement
 * est rejoue (reprise, retry), il est ignore. Indispensable car les topics peuvent
 * etre rejoues depuis l'offset 0 (cf. docs/architecture.md).
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() {
        // Requis par JPA.
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
