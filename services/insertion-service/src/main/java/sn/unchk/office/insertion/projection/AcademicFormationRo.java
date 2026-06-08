package sn.unchk.office.insertion.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-model local des formations, alimenté par le topic {@code academic.formations}.
 * <p>
 * Copie en lecture seule (CQRS) : fournit le libellé et le niveau de la formation pour
 * étiqueter les statistiques d'insertion, sans appel REST vers academic-service.
 */
@Entity
@Table(name = "academic_formation_ro")
public class AcademicFormationRo {

    /** = academic.formations.id. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "level", nullable = false)
    private String level;

    @Column(name = "last_event_at", nullable = false)
    private OffsetDateTime lastEventAt;

    @Column(name = "event_offset")
    private Long eventOffset;

    protected AcademicFormationRo() {
        // Requis par JPA.
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public OffsetDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(OffsetDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public Long getEventOffset() {
        return eventOffset;
    }

    public void setEventOffset(Long eventOffset) {
        this.eventOffset = eventOffset;
    }
}
