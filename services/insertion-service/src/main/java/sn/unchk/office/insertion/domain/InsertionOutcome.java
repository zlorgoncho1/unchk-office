package sn.unchk.office.insertion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Situation d'insertion d'un diplômé à une date de constat (support des statistiques).
 * <p>
 * Permet de mesurer la répartition auto-emploi vs emploi salarié, par formation et par genre
 * (via les read-models {@code people_student_ro} et {@code academic_formation_ro}).
 */
@Entity
@Table(name = "insertion_outcomes")
public class InsertionOutcome {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Étudiant concerné → people.students.id (réf logique). */
    @Column(name = "student_ref", nullable = false)
    private UUID studentRef;

    /** Formation → academic.formations.id (réf logique, pour les stats par formation). */
    @Column(name = "formation_ref")
    private UUID formationRef;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kind", nullable = false, columnDefinition = "insertion_kind")
    private InsertionKind kind;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "observed_at", nullable = false)
    private LocalDate observedAt = LocalDate.now();

    /** Situation courante du diplômé (la dernière connue). */
    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InsertionOutcome() {
        // Requis par JPA.
    }

    @PrePersist
    void avantCreation() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (observedAt == null) {
            observedAt = LocalDate.now();
        }
        OffsetDateTime maintenant = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = maintenant;
        }
        updatedAt = maintenant;
    }

    @PreUpdate
    void avantModification() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentRef() {
        return studentRef;
    }

    public void setStudentRef(UUID studentRef) {
        this.studentRef = studentRef;
    }

    public UUID getFormationRef() {
        return formationRef;
    }

    public void setFormationRef(UUID formationRef) {
        this.formationRef = formationRef;
    }

    public InsertionKind getKind() {
        return kind;
    }

    public void setKind(InsertionKind kind) {
        this.kind = kind;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDate getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDate observedAt) {
        this.observedAt = observedAt;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
