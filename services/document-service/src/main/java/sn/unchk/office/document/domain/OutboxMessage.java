package sn.unchk.office.document.domain;

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
 * Message en attente de publication (pattern Transactional Outbox).
 * <p>
 * Garantit l'atomicité « écriture base + publication Kafka » : on écrit le document ET la
 * ligne d'outbox dans la même transaction. Un relais planifié publie ensuite les lignes non
 * encore envoyées sur le topic {@code document.documents}, puis marque {@code publishedAt}.
 */
@Entity
@Table(name = "outbox")
public class OutboxMessage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    /** Charge utile JSON (état de l'agrégat) déjà sérialisée. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
        // Constructeur requis par JPA.
    }

    public OutboxMessage(String aggregateType, UUID aggregateId, String topic,
                         String eventType, int eventVersion, String payload, String traceId) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.payload = payload;
        this.traceId = traceId;
    }

    @PrePersist
    void avantCreation() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Marque le message comme publié sur Kafka. */
    public void marquerPublie() {
        this.publishedAt = Instant.now();
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

    public int getEventVersion() {
        return eventVersion;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
