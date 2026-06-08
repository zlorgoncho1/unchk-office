package sn.unchk.office.people.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Diplome obtenu par un etudiant.
 * <p>
 * Detail rattache a l'agregat {@link Student} (cascade et suppression orpheline).
 */
@Entity
@Table(name = "student_diplomas")
public class StudentDiploma {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Etudiant proprietaire du diplome. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "level")
    private String level;

    @Column(name = "obtained_at")
    private LocalDate obtainedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    public LocalDate getObtainedAt() {
        return obtainedAt;
    }

    public void setObtainedAt(LocalDate obtainedAt) {
        this.obtainedAt = obtainedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
