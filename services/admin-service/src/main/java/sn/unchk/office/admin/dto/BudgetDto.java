package sn.unchk.office.admin.dto;

import sn.unchk.office.admin.domain.BudgetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue de lecture complète d'un budget (réponse API) : entête + lignes + écart global.
 *
 * @param id              identifiant du budget
 * @param fiscalYear      exercice
 * @param label           libellé
 * @param status          statut
 * @param orientationNote note d'orientation budgétaire
 * @param totalPlanned    total prévu (recalculé depuis les lignes)
 * @param totalRealized   total réalisé (recalculé depuis les lignes)
 * @param ecartGlobal     écart global = prévu − réalisé
 * @param currency        devise
 * @param createdAt       date de création
 * @param updatedAt       date de dernière modification
 * @param lignes          lignes budgétaires détaillées
 */
public record BudgetDto(
        UUID id,
        Short fiscalYear,
        String label,
        BudgetStatus status,
        String orientationNote,
        BigDecimal totalPlanned,
        BigDecimal totalRealized,
        BigDecimal ecartGlobal,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        List<LigneBudgetaireDto> lignes
) {
}
