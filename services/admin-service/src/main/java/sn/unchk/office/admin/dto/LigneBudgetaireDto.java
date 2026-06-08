package sn.unchk.office.admin.dto;

import sn.unchk.office.admin.domain.BudgetLineDirection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vue de lecture d'une ligne budgétaire (réponse API), avec l'écart prévu/réalisé calculé.
 *
 * @param id             identifiant de la ligne
 * @param category       poste
 * @param direction      sens (dépense / recette)
 * @param plannedAmount  montant prévu
 * @param realizedAmount montant réalisé
 * @param ecart          écart = prévu − réalisé (positif = sous-consommation)
 * @param label          libellé
 */
public record LigneBudgetaireDto(
        UUID id,
        String category,
        BudgetLineDirection direction,
        BigDecimal plannedAmount,
        BigDecimal realizedAmount,
        BigDecimal ecart,
        String label
) {
}
