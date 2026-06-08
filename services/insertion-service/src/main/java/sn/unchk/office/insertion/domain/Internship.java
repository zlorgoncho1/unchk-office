package sn.unchk.office.insertion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stage d'un étudiant (support du bilan de stage).
 * <p>
 * Référence l'étudiant par UUID logique ({@code studentRef} → people.students.id), sans clé
 * étrangère inter-base. Le partenaire d'accueil est une vraie FK locale ({@code partnerId}).
 * Le tuteur et le rapport sont des références logiques (people.staff / document.documents).
 */
@Entity
@Table(name = "internships")
public class Internship {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Étudiant concerné → people.students.id (réf logique, validée contre le read-model local). */
    @Column(name = "student_ref", nullable = false)
    private UUID studentRef;

    /** Partenaire d'accueil (FK locale, mise à NULL si le partenaire est supprimé). */
    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "internship_status")
    private InternshipStatus status = InternshipStatus.prevu;

    /** Tuteur académique → people.staff.id (réf logique). */
    @Column(name = "tutor_ref")
    private UUID tutorRef;

    /** Maître de stage côté partenaire (texte libre). */
    @Column(name = "supervisor_name")
    private String supervisorName;

    /** Rapport de stage → document.documents.id (réf logique, binaire dans MinIO). */
    @Column(name = "report_ref")
    private UUID reportRef;

    @Column(name = "grade")
    private BigDecimal grade;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (UUID utilisateur) — sert d'ownerId ABAC. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Internship() {
        // Requis par JPA.
    }

    @PrePersist
    void avantCreation() {
        if (id == null) {
            id = UUID.randomUUID();
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

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public InternshipStatus getStatus() {
        return status;
    }

    public void setStatus(InternshipStatus status) {
        this.status = status;
    }

    public UUID getTutorRef() {
        return tutorRef;
    }

    public void setTutorRef(UUID tutorRef) {
        this.tutorRef = tutorRef;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    public UUID getReportRef() {
        return reportRef;
    }

    public void setReportRef(UUID reportRef) {
        this.reportRef = reportRef;
    }

    public BigDecimal getGrade() {
        return grade;
    }

    public void setGrade(BigDecimal grade) {
        this.grade = grade;
    }

    public long getVersion() {
        return version;
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
