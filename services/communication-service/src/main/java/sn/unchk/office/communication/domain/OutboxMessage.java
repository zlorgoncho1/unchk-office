package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Message en attente de publication sur Kafka (Transactional Outbox).
 * <p>
 * Écrit dans la même transaction que la modification de l'agrégat, ce qui garantit
 * l'atomicité « écriture base + publication événement ». Un relais périodique lit les
 * lignes non publiées et les émet sur le topic, puis pose {@code publishedAt}.
 */
@Entity
@Table(name = "outbox")
public class OutboxMessage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Type d'agrégat (Reunion, CompteRendu, Notification). */
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /** Identifiant de l'agrégat = clé de partition Kafka. */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /** Topic de destination. */
    @Column(name = "topic", nullable = false)
    private String topic;

    /** Type d'événement métier (ReunionPlanifiee, CompteRenduPublie...). */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Identifiant de corrélation propagé de bout en bout. */
    @Column(name = "trace_id")
    private String traceId;

    /** État de l'agrégat sérialisé en JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** NULL tant que le message n'a pas été relayé vers Kafka. */
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
        // Requis par JPA.
    }

    public OutboxMessage(String aggregateType, UUID aggregateId, String topic,
                         String eventType, String traceId, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventType = eventType;
        this.traceId = traceId;
        this.payload = payload;
    }

    @PrePersist
    void aLaCreation() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void marquerPublie() {
        this.publishedAt = Instant.now();
    }
}
