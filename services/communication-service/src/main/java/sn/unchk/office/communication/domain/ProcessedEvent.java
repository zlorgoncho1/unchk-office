package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace des événements Kafka déjà consommés (idempotence côté consommateur).
 * <p>
 * Avant d'appliquer un événement à un read-model, le consommateur vérifie que son
 * {@code eventId} n'a pas déjà été traité. Indispensable car les topics peuvent rejouer.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    /** Identifiant de l'événement (header {@code eventId} de l'enveloppe). */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
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
