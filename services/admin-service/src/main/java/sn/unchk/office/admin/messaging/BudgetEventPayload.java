package sn.unchk.office.admin.messaging;

import sn.unchk.office.admin.domain.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Charge utile (state transfer) publiée sur le topic {@code admin.budget}.
 * <p>
 * Ne transporte que l'état métier de l'agrégat budget (pas de secret, pas d'objet JPA).
 * Les consommateurs (academic, communication...) construisent leur read-model à partir d'elle.
 *
 * @param budgetId      identifiant du budget
 * @param fiscalYear    exercice
 * @param label         libellé
 * @param status        statut
 * @param totalPlanned  total prévu
 * @param totalRealized total réalisé
 * @param currency      devise
 */
public record BudgetEventPayload(
        UUID budgetId,
        Short fiscalYear,
        String label,
        BudgetStatus status,
        BigDecimal totalPlanned,
        BigDecimal totalRealized,
        String currency
) {
}
