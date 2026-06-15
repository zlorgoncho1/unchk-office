package sn.unchk.office.admin.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Communiqué administratif : note de service ou circulaire du niveau central.
 * <p>
 * Agrégat racine de la diffusion administrative (module Administration → gestion documentaire).
 * Le ciblage par rôle ({@code targets}) détermine les destinataires : à la publication
 * ({@code isPublished}), un événement est émis sur {@code admin.communiques} et le
 * communication-service notifie les rôles ciblés (« notification automatique à chaque nouvelle
 * circulaire / note de service »). Suppression logique via {@code deletedAt}.
 */
@Entity
@Table(name = "admin_communiques")
public class AdminCommunique {

    /** Identifiant opaque (UUID, anti-énumération). */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Nature (note de service / circulaire). */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "kind", nullable = false, columnDefinition = "admin_doc_kind")
    private AdminDocKind kind;

    /** Référence / numéro (unique si renseignée). */
    @Column(name = "reference")
    private String reference;

    /** Titre / objet. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Corps du communiqué. */
    @Column(name = "body")
    private String body;

    /** Pièce jointe éventuelle (→ document.documents.id). */
    @Column(name = "document_ref")
    private UUID documentRef;

    /** Date d'émission (par défaut : aujourd'hui). */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /** Indicateur de publication (déclenche les notifications). */
    @Column(name = "is_published", nullable = false)
    private boolean published;

    /** Horodatage de publication. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Rôles destinataires (table {@code communique_targets}). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "communique_targets",
            joinColumns = @JoinColumn(name = "communique_id"))
    @Column(name = "role", nullable = false)
    private Set<String> targets = new LinkedHashSet<>();

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur (→ identity.users.id) — sert aussi de propriétaire ABAC. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Horodatage de suppression logique (NULL = actif). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public AdminCommunique() {
        // Requis par JPA.
    }

    /** Affecte l'identifiant et les horodatages avant la première persistance. */
    @PrePersist
    void avantPersistance() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant maintenant = Instant.now();
        if (createdAt == null) {
            createdAt = maintenant;
        }
        updatedAt = maintenant;
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public AdminDocKind getKind() {
        return kind;
    }

    public void setKind(AdminDocKind kind) {
        this.kind = kind;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
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

    public Set<String> getTargets() {
        return targets;
    }

    public void setTargets(Set<String> targets) {
        this.targets = targets;
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
