package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Données d'entrée pour renseigner le montant réalisé d'une ligne budgétaire
 * (saisie du budget réalisé en regard du projet).
 *
 * @param realizedAmount montant réalisé (≥ 0)
 */
public record RealisationLigneDto(
        @NotNull(message = "Le montant réalisé est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le montant réalisé doit être positif ou nul.")
        @Digits(integer = 14, fraction = 2, message = "Montant invalide (14 chiffres, 2 décimales max).")
        BigDecimal realizedAmount
) {
}
