package sn.unchk.office.insertion.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Journal des événements Kafka déjà traités (dédoublonnage / idempotence).
 * <p>
 * Avant d'appliquer un événement à un read-model, le consommateur vérifie que son
 * {@code eventId} n'a pas déjà été traité. On évite ainsi les doublons en cas de
 * relivraison Kafka (at-least-once).
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    /** Identifiant unique de l'événement (= DomainEvent.eventId). */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
        // Requis par JPA.
    }

    public ProcessedEvent(UUID eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
        this.processedAt = OffsetDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTopic() {
        return topic;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
