package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données de mise à jour d'un courrier (attributs modifiables).
 * <p>
 * Le sens ({@code direction}) n'est pas modifiable après enregistrement ; le statut suit un
 * endpoint dédié ({@code PATCH /{id}/statut}).
 *
 * @param subject       objet
 * @param correspondent correspondant
 * @param mailDate      date du courrier
 * @param assignedTo    agent en charge (optionnel)
 * @param reference     référence (optionnel)
 * @param notes         annotations (optionnel)
 */
public record MajMailDto(
        @NotBlank(message = "L'objet est obligatoire.")
        @Size(max = 2000, message = "L'objet est trop long.")
        String subject,

        @NotBlank(message = "Le correspondant est obligatoire.")
        @Size(max = 500, message = "Le correspondant est trop long.")
        String correspondent,

        @NotNull(message = "La date du courrier est obligatoire.")
        LocalDate mailDate,

        UUID assignedTo,

        @Size(max = 64, message = "La référence est trop longue.")
        String reference,

        @Size(max = 4000, message = "Les annotations sont trop longues.")
        String notes
) {
}
