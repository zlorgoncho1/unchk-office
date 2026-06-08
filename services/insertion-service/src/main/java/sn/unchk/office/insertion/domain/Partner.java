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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Partenaire (structure d'accueil des étudiants : entreprise, administration, ONG...).
 * <p>
 * Agrégat propre au service. Clé primaire UUID (anti-IDOR). Suppression logique via
 * {@code deletedAt} et verrouillage optimiste via {@code version}.
 */
@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    /** Type de partenaire, mappé sur le type énuméré PostgreSQL {@code partner_kind}. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kind", nullable = false, columnDefinition = "partner_kind")
    private PartnerKind kind = PartnerKind.entreprise;

    @Column(name = "sector")
    private String sector;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (UUID utilisateur d'identity-service) — sert d'ownerId ABAC. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Suppression logique : non nul = partenaire désactivé/supprimé. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Partner() {
        // Requis par JPA.
    }

    /** Initialise l'identifiant et les horodatages avant la première écriture. */
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

    /** Met à jour l'horodatage de modification. */
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PartnerKind getKind() {
        return kind;
    }

    public void setKind(PartnerKind kind) {
        this.kind = kind;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
