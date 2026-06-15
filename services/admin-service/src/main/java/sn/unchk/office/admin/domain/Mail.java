package sn.unchk.office.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Courrier administratif (arrivé / départ) — registre du courrier.
 * <p>
 * Agrégat racine de la gestion du courrier (module Administration → gestion documentaire).
 * La traçabilité s'appuie sur {@code reference} (numéro de courrier) et {@code status}
 * (cycle de vie : reçu → en traitement → traité → archivé/clos). La pièce scannée éventuelle
 * est référencée par {@code documentRef} (→ document-service / MinIO). L'agent en charge est
 * désigné par {@code assignedTo} (→ people.staff.id). Suppression logique via {@code deletedAt}.
 */
@Entity
@Table(name = "mails")
public class Mail {

    /** Identifiant opaque (UUID, anti-énumération). */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Référence / numéro de courrier (unique si renseignée). */
    @Column(name = "reference")
    private String reference;

    /** Sens du courrier (arrivé / départ). */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "direction", nullable = false, columnDefinition = "mail_direction")
    private MailDirection direction;

    /** Objet du courrier. */
    @Column(name = "subject", nullable = false)
    private String subject;

    /** Correspondant (expéditeur si arrivé, destinataire si départ). */
    @Column(name = "correspondent", nullable = false)
    private String correspondent;

    /** Date du courrier. */
    @Column(name = "mail_date", nullable = false)
    private LocalDate mailDate;

    /** Date d'enregistrement au registre (par défaut : aujourd'hui). */
    @Column(name = "registered_at", nullable = false)
    private LocalDate registeredAt;

    /** Statut de traitement. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "mail_status")
    private MailStatus status = MailStatus.recu;

    /** Agent en charge (→ people.staff.id). */
    @Column(name = "assigned_to")
    private UUID assignedTo;

    /** Pièce scannée associée (→ document.documents.id). */
    @Column(name = "document_ref")
    private UUID documentRef;

    /** Annotations libres. */
    @Column(name = "notes")
    private String notes;

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de l'enregistrement (→ identity.users.id) — sert aussi de propriétaire ABAC. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Horodatage de suppression logique (NULL = actif). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Mail() {
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
        if (registeredAt == null) {
            registeredAt = LocalDate.now();
        }
        if (status == null) {
            status = MailStatus.recu;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public MailDirection getDirection() {
        return direction;
    }

    public void setDirection(MailDirection direction) {
        this.direction = direction;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCorrespondent() {
        return correspondent;
    }

    public void setCorrespondent(String correspondent) {
        this.correspondent = correspondent;
    }

    public LocalDate getMailDate() {
        return mailDate;
    }

    public void setMailDate(LocalDate mailDate) {
        this.mailDate = mailDate;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }

    public MailStatus getStatus() {
        return status;
    }

    public void setStatus(MailStatus status) {
        this.status = status;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
