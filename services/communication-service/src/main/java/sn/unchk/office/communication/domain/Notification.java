package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification destinée à un utilisateur (badge + historique), poussée en temps réel
 * via WebSocket lorsque la session du destinataire est active.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Destinataire (réf. logique identity.users.id). */
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    /** Catégorie de la notification. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "kind", nullable = false, columnDefinition = "notification_kind")
    private NotificationKind kind;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message")
    private String message;

    /** Service cible pour le deep-link (ex : "communication"). */
    @Column(name = "target_service")
    private String targetService;

    /** Ressource cible pour le deep-link (UUID). */
    @Column(name = "target_ref")
    private UUID targetRef;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    /** Indique si la notification a été poussée via WebSocket. */
    @Column(name = "delivered_ws", nullable = false)
    private boolean deliveredWs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {
        // Requis par JPA.
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

    // --- Accesseurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public NotificationKind getKind() {
        return kind;
    }

    public void setKind(NotificationKind kind) {
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public UUID getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(UUID targetRef) {
        this.targetRef = targetRef;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public boolean isDeliveredWs() {
        return deliveredWs;
    }

    public void setDeliveredWs(boolean deliveredWs) {
        this.deliveredWs = deliveredWs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
