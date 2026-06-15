package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Données de mise à jour d'un communiqué (la nature n'est pas modifiable après création).
 *
 * @param title     titre / objet
 * @param body      corps (optionnel)
 * @param audience  audience cible (préréglage de rôles)
 * @param issueDate date d'émission
 * @param reference référence (optionnelle)
 */
public record MajCommuniqueDto(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 2000, message = "Le titre est trop long.")
        String title,

        @Size(max = 8000, message = "Le corps est trop long.")
        String body,

        @NotBlank(message = "L'audience est obligatoire.")
        String audience,

        LocalDate issueDate,

        @Size(max = 64, message = "La référence est trop longue.")
        String reference
) {
}
