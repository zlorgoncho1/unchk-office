package sn.unchk.office.communication.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Compte rendu d'une réunion / d'un événement.
 * <p>
 * Agrégat maître. À la publication ({@code is_published = true}), un événement
 * {@code CompteRenduPublie} est émis sur {@code communication.comptesrendus} (Outbox),
 * ce qui déclenche la résolution des destinataires et l'envoi de notifications.
 * <p>
 * La visibilité par rôle ({@code compte_rendu_visibility}) alimente l'ABAC anti-IDOR :
 * un utilisateur ne consulte un compte rendu que si l'un de ses rôles y figure, ou s'il en
 * est le propriétaire ({@code createdBy}).
 */
@Entity
@Table(name = "comptes_rendus")
public class CompteRendu {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Réunion source (optionnelle, mise à NULL si la réunion est supprimée). */
    @Column(name = "reunion_id")
    private UUID reunionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false, columnDefinition = "meeting_type")
    private MeetingType type;

    /** Contenu rédigé. */
    @Column(name = "body")
    private String body;

    /** Document PDF archivé (réf. logique document.documents.id). */
    @Column(name = "document_ref")
    private UUID documentRef;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    /** Rédacteur (réf. logique people.staff.id). */
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (réf. identity.users.id). Propriétaire ABAC (ownerId). */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Rôles autorisés à consulter ce compte rendu (visibilité ABAC).
     * Stockés dans la table {@code compte_rendu_visibility}.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "compte_rendu_visibility",
            joinColumns = @JoinColumn(name = "compte_rendu_id"))
    @Column(name = "role", nullable = false)
    private Set<String> visibility = new HashSet<>();

    public CompteRendu() {
        // Requis par JPA.
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

    public UUID getReunionId() {
        return reunionId;
    }

    public void setReunionId(UUID reunionId) {
        this.reunionId = reunionId;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
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

    public Set<String> getVisibility() {
        return visibility;
    }

    public void setVisibility(Set<String> visibility) {
        this.visibility = visibility != null ? visibility : new HashSet<>();
    }
}
