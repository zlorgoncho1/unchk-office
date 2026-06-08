package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotNull;
import sn.unchk.office.admin.domain.BudgetStatus;

/**
 * Données d'entrée pour faire évoluer le statut d'un budget
 * (projet → voté → en exécution → clôturé).
 *
 * @param status nouveau statut souhaité
 */
public record ChangementStatutBudgetDto(
        @NotNull(message = "Le statut cible est obligatoire.")
        BudgetStatus status
) {
}
