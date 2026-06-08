package sn.unchk.office.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ligne budgétaire : montant prévu vs réalisé pour un poste donné (dépense ou recette).
 * <p>
 * Rattachée à un {@link Budget} par son identifiant ({@code budgetId}). On ne déclare
 * pas d'association JPA bidirectionnelle pour rester simple ; le rattachement se fait par UUID.
 */
@Entity
@Table(name = "budget_lines")
public class BudgetLine {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Budget parent (→ budgets.id). */
    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    /** Poste de dépense / recette. */
    @Column(name = "category", nullable = false)
    private String category;

    /** Sens de la ligne (dépense / recette). */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private BudgetLineDirection direction;

    /** Montant prévu pour ce poste. */
    @Column(name = "planned_amount", nullable = false)
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    /** Montant réalisé pour ce poste. */
    @Column(name = "realized_amount", nullable = false)
    private BigDecimal realizedAmount = BigDecimal.ZERO;

    /** Libellé optionnel. */
    @Column(name = "label")
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BudgetLine() {
        // Requis par JPA.
    }

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

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BudgetLineDirection getDirection() {
        return direction;
    }

    public void setDirection(BudgetLineDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public BigDecimal getRealizedAmount() {
        return realizedAmount;
    }

    public void setRealizedAmount(BigDecimal realizedAmount) {
        this.realizedAmount = realizedAmount;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
