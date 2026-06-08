package sn.unchk.office.academic.formateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Données d'affectation d'un formateur à une formation pour un module
 * (corps de {@code POST /api/academic/formations/{id}/formateurs}).
 *
 * @param formateurRef référence du formateur (people.staff.id), obligatoire
 * @param module       matière enseignée, obligatoire
 */
public record AffectationCreationDto(
        @NotNull(message = "La référence du formateur est obligatoire.")
        UUID formateurRef,

        @NotBlank(message = "Le module (matière) est obligatoire.")
        String module
) {
}
