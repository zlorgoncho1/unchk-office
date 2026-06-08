package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Évènement Kafka déjà traité (table {@code processed_events}).
 * <p>
 * Garantit l'idempotence des consommateurs : un {@code eventId} déjà enregistré ne sera
 * pas retraité lors d'un rejeu du topic. Indispensable car les topics peuvent être relus.
 */
@Entity
@Table(name = "processed_events")
public class EvenementTraite {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected EvenementTraite() {
        // Requis par JPA.
    }

    public EvenementTraite(UUID eventId) {
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
