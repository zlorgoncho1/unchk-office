package sn.unchk.office.admin.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace des événements Kafka déjà traités (idempotence du consommateur).
 * <p>
 * Avant d'appliquer un événement à un read-model, le consommateur vérifie que son
 * {@code eventId} n'a pas déjà été traité, car un topic peut rejouer (cf. docs/architecture.md §8).
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    /** Identifiant de l'événement (issu de l'enveloppe DomainEvent). */
    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

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
