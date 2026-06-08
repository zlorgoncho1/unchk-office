package sn.unchk.office.admin.dto;

import sn.unchk.office.admin.domain.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vue résumée d'un budget (réponse API de liste), sans le détail des lignes.
 *
 * @param id            identifiant du budget
 * @param fiscalYear    exercice
 * @param label         libellé
 * @param status        statut
 * @param totalPlanned  total prévu
 * @param totalRealized total réalisé
 * @param currency      devise
 */
public record BudgetResumeDto(
        UUID id,
        Short fiscalYear,
        String label,
        BudgetStatus status,
        BigDecimal totalPlanned,
        BigDecimal totalRealized,
        String currency
) {
}
