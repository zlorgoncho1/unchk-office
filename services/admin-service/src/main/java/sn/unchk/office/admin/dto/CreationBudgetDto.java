package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Données d'entrée pour créer un projet de budget.
 * <p>
 * DTO dédié (jamais l'entité JPA en {@code @RequestBody}) : les champs système
 * ({@code id}, {@code ownerId}, {@code createdAt}, totaux) ne sont jamais liés depuis le client.
 *
 * @param fiscalYear      exercice budgétaire (ex : 2026)
 * @param label           libellé du budget
 * @param orientationNote note d'orientation budgétaire (optionnelle)
 * @param currency        devise ISO 4217 (3 lettres) ; XOF si absente
 */
public record CreationBudgetDto(
        @NotNull(message = "L'exercice est obligatoire.")
        @Min(value = 2000, message = "L'exercice doit être postérieur à 2000.")
        @Max(value = 2100, message = "L'exercice doit être antérieur à 2100.")
        Short fiscalYear,

        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 255, message = "Le libellé est trop long.")
        String label,

        @Size(max = 4000, message = "La note d'orientation est trop longue.")
        String orientationNote,

        @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit comporter 3 lettres majuscules (ISO 4217).")
        String currency
) {
}
