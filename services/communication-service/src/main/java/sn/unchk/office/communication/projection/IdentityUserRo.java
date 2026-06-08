package sn.unchk.office.communication.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model local des utilisateurs (projection du topic {@code identity.users}).
 * <p>
 * Permet de résoudre les destinataires d'une notification par rôle, sans aucun appel REST
 * vers identity-service. Table {@code identity_user_ro} (lecture seule pour l'API).
 */
@Entity
@Table(name = "identity_user_ro")
public class IdentityUserRo {

    /** = identity.users.id. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", columnDefinition = "citext")
    private String email;

    /** Rôles applicatifs de l'utilisateur (tableau PostgreSQL {@code text[]}). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", nullable = false, columnDefinition = "text[]")
    private String[] roles = new String[0];

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    /** Décalage Kafka du dernier événement appliqué (idempotence). */
    @Column(name = "event_offset")
    private Long eventOffset;

    protected IdentityUserRo() {
        // Requis par JPA.
    }

    public IdentityUserRo(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles != null ? roles : new String[0];
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public Long getEventOffset() {
        return eventOffset;
    }

    public void setEventOffset(Long eventOffset) {
        this.eventOffset = eventOffset;
    }
}
