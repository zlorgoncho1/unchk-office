package sn.unchk.office.communication.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model local du personnel (projection du topic {@code people.staff}).
 * <p>
 * Sert à afficher le nom de l'auteur d'un compte rendu ou de l'organisateur d'une réunion
 * sans appel REST vers people-service. Table {@code people_staff_ro} (lecture seule).
 */
@Entity
@Table(name = "people_staff_ro")
public class PeopleStaffRo {

    /** = people.staff.id. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    /** Décalage Kafka du dernier événement appliqué (idempotence). */
    @Column(name = "event_offset")
    private Long eventOffset;

    protected PeopleStaffRo() {
        // Requis par JPA.
    }

    public PeopleStaffRo(UUID id) {
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
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
