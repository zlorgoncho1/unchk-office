package sn.unchk.office.insertion.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-model local des étudiants, alimenté par le topic {@code people.students}.
 * <p>
 * Copie en lecture seule (CQRS) : JAMAIS écrite par l'API, uniquement par le consommateur Kafka.
 * Sert à produire les statistiques d'insertion (par genre, par formation) sans appel REST.
 */
@Entity
@Table(name = "people_student_ro")
public class PeopleStudentRo {

    /** = people.students.id (identifiant canonique de l'étudiant). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Genre de l'étudiant (utilisé pour les statistiques par genre). */
    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "formation_ref")
    private UUID formationRef;

    @Column(name = "promotion", length = 32)
    private String promotion;

    @Column(name = "exit_year")
    private Short exitYear;

    @Column(name = "last_event_at", nullable = false)
    private OffsetDateTime lastEventAt;

    /** Offset Kafka du dernier événement appliqué (idempotence). */
    @Column(name = "event_offset")
    private Long eventOffset;

    public PeopleStudentRo() {
        // Requis par JPA.
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public UUID getFormationRef() {
        return formationRef;
    }

    public void setFormationRef(UUID formationRef) {
        this.formationRef = formationRef;
    }

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public Short getExitYear() {
        return exitYear;
    }

    public void setExitYear(Short exitYear) {
        this.exitYear = exitYear;
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
