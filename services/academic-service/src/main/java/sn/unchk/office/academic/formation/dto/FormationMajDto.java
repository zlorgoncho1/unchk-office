package sn.unchk.office.academic.formation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import sn.unchk.office.academic.formation.Financement;
import sn.unchk.office.academic.formation.NiveauFormation;
import sn.unchk.office.academic.formation.TypeFormation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données de mise à jour d'une formation (corps de {@code PUT /api/academic/formations/{id}}).
 * Mise à jour complète : tous les champs modifiables sont fournis.
 *
 * @param code           code unique (optionnel)
 * @param label          intitulé obligatoire
 * @param level          niveau obligatoire
 * @param kind           type
 * @param funding        financement
 * @param startDate      date de début
 * @param endDate        date de fin
 * @param trainedMale    formés (hommes)
 * @param trainedFemale  formés (femmes)
 * @param responsibleRef responsable (people.staff.id)
 * @param active         formation active
 */
public record FormationMajDto(
        @Size(max = 32, message = "Le code ne doit pas dépasser 32 caractères.")
        String code,

        @NotBlank(message = "L'intitulé de la formation est obligatoire.")
        String label,

        @NotNull(message = "Le niveau de la formation est obligatoire.")
        NiveauFormation level,

        TypeFormation kind,

        Financement funding,

        LocalDate startDate,

        LocalDate endDate,

        @PositiveOrZero(message = "Le nombre de formés (hommes) doit être positif ou nul.")
        Integer trainedMale,

        @PositiveOrZero(message = "Le nombre de formés (femmes) doit être positif ou nul.")
        Integer trainedFemale,

        UUID responsibleRef,

        Boolean active
) {
}
