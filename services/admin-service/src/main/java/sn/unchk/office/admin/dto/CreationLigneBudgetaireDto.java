package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.admin.domain.BudgetLineDirection;

import java.math.BigDecimal;

/**
 * Données d'entrée pour ajouter une ligne budgétaire (poste prévu) à un budget.
 *
 * @param category      poste de dépense / recette
 * @param direction     sens (dépense / recette)
 * @param plannedAmount montant prévu (≥ 0)
 * @param label         libellé optionnel
 */
public record CreationLigneBudgetaireDto(
        @NotBlank(message = "Le poste (catégorie) est obligatoire.")
        @Size(max = 255, message = "Le poste est trop long.")
        String category,

        @NotNull(message = "Le sens (dépense/recette) est obligatoire.")
        BudgetLineDirection direction,

        @NotNull(message = "Le montant prévu est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le montant prévu doit être positif ou nul.")
        @Digits(integer = 14, fraction = 2, message = "Montant invalide (14 chiffres, 2 décimales max).")
        BigDecimal plannedAmount,

        @Size(max = 255, message = "Le libellé est trop long.")
        String label
) {
}
