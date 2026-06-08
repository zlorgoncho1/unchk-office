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
 * Données de création d'une formation (corps de requête {@code POST /api/academic/formations}).
 * <p>
 * Validation déclarative (Jakarta Bean Validation) : le contrôleur refuse en 400 toute donnée
 * invalide avant d'atteindre la couche métier.
 *
 * @param code           code unique (optionnel), 32 caractères max
 * @param label          intitulé obligatoire
 * @param level          niveau obligatoire
 * @param kind           type (optionnel, défaut {@code initiale} côté entité)
 * @param funding        source de financement (optionnelle)
 * @param startDate      date de début (optionnelle)
 * @param endDate        date de fin (optionnelle, validée >= startDate côté service)
 * @param trainedMale    formés (hommes), >= 0
 * @param trainedFemale  formés (femmes), >= 0
 * @param responsibleRef référence du responsable (people.staff.id), optionnelle
 */
public record FormationCreationDto(
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

        UUID responsibleRef
) {
}
