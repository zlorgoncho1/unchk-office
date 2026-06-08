package sn.unchk.office.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Métadonnées d'un document. Le binaire réel vit dans MinIO ; cette entité ne porte
 * que le couple {@code (bucket, objectKey)} pour le retrouver, plus les attributs ABAC
 * ({@code ownerId}) qui alimentent l'anti-IDOR OPA.
 * <p>
 * Clé primaire UUID (anti-énumération). Verrou optimiste via {@code version}.
 */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Convert(converter = ConvertisseurCategorie.class)
    @Column(name = "category", nullable = false)
    private CategorieDocument category;

    @Column(name = "description")
    private String description;

    @Column(name = "bucket", nullable = false)
    private String bucket;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    /** Propriétaire du document (→ identity.users.id) — clé ABAC anti-IDOR (ownerId OPA). */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "source_ref")
    private UUID sourceRef;

    /** Verrou optimiste : empêche les mises à jour concurrentes silencieuses. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Date de suppression logique (soft delete) ; null tant que le document est actif. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Document() {
        // Constructeur requis par JPA.
    }

    /** Renseigne les horodatages à la création. */
    @PrePersist
    void avantCreation() {
        Instant maintenant = Instant.now();
        if (createdAt == null) {
            createdAt = maintenant;
        }
        updatedAt = maintenant;
    }

    /** Rafraîchit l'horodatage de modification. */
    @PreUpdate
    void avantMiseAJour() {
        updatedAt = Instant.now();
    }

    // --- Accès ---

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

    public CategorieDocument getCategory() {
        return category;
    }

    public void setCategory(CategorieDocument category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public UUID getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(UUID sourceRef) {
        this.sourceRef = sourceRef;
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
