package sn.unchk.office.admin.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model local du personnel (projection CQRS du topic {@code people.staff}).
 * <p>
 * Table {@code people_staff_ro} alimentée UNIQUEMENT par le consommateur Kafka, jamais par l'API.
 * Permet d'afficher l'agent en charge d'un courrier (ou un auteur) sans aucun appel REST vers
 * people-service. Les colonnes {@code event_offset} / {@code last_event_at} assurent l'idempotence.
 */
@Entity
@Table(name = "people_staff_ro")
public class PeopleStaffRo {

    /** Identifiant = people.staff.id. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "department")
    private String department;

    /** Horodatage du dernier événement consommé pour cette clé. */
    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    /** Offset du dernier événement consommé (idempotence Kafka). */
    @Column(name = "event_offset")
    private Long eventOffset;

    protected PeopleStaffRo() {
        // Requis par JPA.
    }

    public PeopleStaffRo(UUID id, String fullName, String kind, String department,
                         Instant lastEventAt, Long eventOffset) {
        this.id = id;
        this.fullName = fullName;
        this.kind = kind;
        this.department = department;
        this.lastEventAt = lastEventAt;
        this.eventOffset = eventOffset;
    }

    public UUID getId() {
        return id;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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
