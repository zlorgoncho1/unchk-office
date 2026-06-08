package sn.unchk.office.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model local du compte utilisateur (projection CQRS du topic {@code identity.users}).
 * <p>
 * Le document-service ne fait JAMAIS d'appel REST vers identity-service : il maintient sa
 * propre copie en lecture seule, alimentée par Kafka. Cette projection sert, par exemple, à
 * connaître les rôles d'un destinataire ou à valider la cohérence d'un propriétaire.
 */
@Entity
@Table(name = "identity_user_ro")
public class IdentityUserRo {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Rôles applicatifs séparés par des virgules (ex : {@code administratif,enseignant}). */
    @Column(name = "roles")
    private String roles;

    @Column(name = "status")
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityUserRo() {
        // Constructeur requis par JPA.
    }

    public IdentityUserRo(UUID id, String roles, String status, Instant updatedAt) {
        this.id = id;
        this.roles = roles;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
