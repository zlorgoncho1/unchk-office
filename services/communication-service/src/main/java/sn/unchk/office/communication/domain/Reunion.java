package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Réunion / événement (réunion, séminaire, webinaire, Conseil d'Université, tutorat...).
 * <p>
 * Agrégat maître de ce service. Sa modification émet un événement sur
 * {@code communication.reunions} (via l'Outbox).
 */
@Entity
@Table(name = "reunions")
public class Reunion {

    /** Clé primaire UUID (anti-IDOR). Générée en base par défaut, ou côté applicatif. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Titre de la réunion. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Type de réunion. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false, columnDefinition = "meeting_type")
    private MeetingType type = MeetingType.reunion;

    /** Description libre. */
    @Column(name = "description")
    private String description;

    /** Salle ou lien de visioconférence. */
    @Column(name = "location")
    private String location;

    /** Début de la réunion. */
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    /** Fin de la réunion (optionnelle). */
    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    /** Statut courant. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "meeting_status")
    private MeetingStatus status = MeetingStatus.planifiee;

    /** Organisateur (réf. logique people.staff.id). */
    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    /** Formation liée (réf. logique academic.formations.id), optionnelle. */
    @Column(name = "formation_ref")
    private UUID formationRef;

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (réf. identity.users.id). Sert de propriétaire ABAC. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Suppression logique. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Reunion() {
        // Constructeur requis par JPA.
    }

    @PrePersist
    void aLaCreation() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant maintenant = Instant.now();
        createdAt = maintenant;
        updatedAt = maintenant;
    }

    @PreUpdate
    void aLaMiseAJour() {
        updatedAt = Instant.now();
    }

    // --- Accesseurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MeetingType getType() {
        return type;
    }

    public void setType(MeetingType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(UUID organizerId) {
        this.organizerId = organizerId;
    }

    public UUID getFormationRef() {
        return formationRef;
    }

    public void setFormationRef(UUID formationRef) {
        this.formationRef = formationRef;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
