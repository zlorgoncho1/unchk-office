package sn.unchk.office.people.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model (projection CQRS) du topic {@code identity.users}.
 * <p>
 * Table en LECTURE SEULE pour l'API : alimentee uniquement par le consommateur Kafka.
 * Permet a people-service de relier un compte utilisateur a son etudiant
 * (pour resoudre la fiche "me" anti-IDOR) et d'afficher l'auteur des fiches,
 * sans aucun appel REST vers identity-service.
 */
@Entity
@Table(name = "identity_user_ro")
public class IdentityUserRo {

    /** = identity.users.id */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    /** Roles de l'utilisateur (tableau texte PostgreSQL). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", nullable = false, columnDefinition = "text[]")
    private String[] roles = new String[0];

    /** Reference vers la personne liee (people.students.id | people.staff.id). */
    @Column(name = "person_ref")
    private UUID personRef;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    /** Idempotence Kafka : dernier offset consomme pour cette cle. */
    @Column(name = "event_offset")
    private Long eventOffset;

    // --- Accesseurs ---

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

    public UUID getPersonRef() {
        return personRef;
    }

    public void setPersonRef(UUID personRef) {
        this.personRef = personRef;
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
