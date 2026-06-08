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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Budget d'un exercice : projet de budget puis budget réalisé.
 * <p>
 * Agrégat racine de la gestion budgétaire. Porte la note d'orientation budgétaire
 * ({@code orientationNote}) et les totaux prévu / réalisé. Référencé par les
 * {@link BudgetLine lignes budgétaires}. La colonne {@code owner_id} alimente l'ABAC
 * anti-IDOR exposé à OPA.
 */
@Entity
@Table(name = "budgets")
public class Budget {

    /** Identifiant opaque (UUID, anti-énumération). */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Exercice budgétaire (ex : 2026). */
    @Column(name = "fiscal_year", nullable = false)
    private Short fiscalYear;

    /** Libellé du budget. */
    @Column(name = "label", nullable = false)
    private String label;

    /** Statut du cycle de vie (projet, voté, en exécution, clôturé). */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "budget_status")
    private BudgetStatus status = BudgetStatus.projet;

    /** Note d'orientation budgétaire associée à l'exercice. */
    @Column(name = "orientation_note")
    private String orientationNote;

    /** Total prévu (somme des montants prévus). */
    @Column(name = "total_planned", nullable = false)
    private BigDecimal totalPlanned = BigDecimal.ZERO;

    /** Total réalisé (somme des montants réalisés). */
    @Column(name = "total_realized", nullable = false)
    private BigDecimal totalRealized = BigDecimal.ZERO;

    /** Devise (ISO 4217, par défaut XOF). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "XOF";

    /** Propriétaire de la ressource (→ identity.users.id) — ABAC anti-IDOR. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (→ identity.users.id). */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Budget() {
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
    }

    public UUID getId() {
        return id;
    }

    public Short getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Short fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public String getOrientationNote() {
        return orientationNote;
    }

    public void setOrientationNote(String orientationNote) {
        this.orientationNote = orientationNote;
    }

    public BigDecimal getTotalPlanned() {
        return totalPlanned;
    }

    public void setTotalPlanned(BigDecimal totalPlanned) {
        this.totalPlanned = totalPlanned;
    }

    public BigDecimal getTotalRealized() {
        return totalRealized;
    }

    public void setTotalRealized(BigDecimal totalRealized) {
        this.totalRealized = totalRealized;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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
}
