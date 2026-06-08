package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Données d'entrée pour mettre à jour les attributs modifiables d'un budget
 * (libellé, note d'orientation, devise). Les totaux sont recalculés à partir des lignes.
 *
 * @param label           nouveau libellé
 * @param orientationNote note d'orientation budgétaire
 * @param currency        devise ISO 4217
 */
public record MajBudgetDto(
        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 255, message = "Le libellé est trop long.")
        String label,

        @Size(max = 4000, message = "La note d'orientation est trop longue.")
        String orientationNote,

        @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit comporter 3 lettres majuscules (ISO 4217).")
        String currency
) {
}
