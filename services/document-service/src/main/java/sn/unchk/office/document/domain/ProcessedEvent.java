package sn.unchk.office.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'idempotence : enregistre les eventId déjà traités par les consommateurs Kafka.
 * <p>
 * Indispensable car un topic peut rejouer (reprise, nouveau consommateur). Avant d'appliquer
 * un événement sur un read-model, on vérifie l'absence de l'eventId dans cette table.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
        // Constructeur requis par JPA.
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
    }

    @PrePersist
    void avantCreation() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
