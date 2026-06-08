package sn.unchk.office.academic.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'idempotence des événements Kafka déjà traités (table {@code processed_events}).
 * <p>
 * Avant d'appliquer un événement à un read-model, le consommateur vérifie que son
 * {@code eventId} n'a pas déjà été traité ; cela protège contre les rejeux (topics
 * compactés rejoués depuis l'offset 0, retries, etc.).
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    /** Identifiant de l'événement déjà appliqué (= DomainEvent.eventId). */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    /** Horodatage de traitement. */
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
        // Constructeur requis par JPA.
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
