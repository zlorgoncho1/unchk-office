package sn.unchk.office.academic.emploidutemps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.unchk.office.academic.emploidutemps.JourSemaine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Données de création d'un créneau d'emploi du temps
 * (corps de {@code POST /api/academic/formations/{id}/creneaux}).
 * <p>
 * Règle métier (validée côté service) : un créneau est soit récurrent ({@code dayOfWeek}),
 * soit ponctuel ({@code sessionDate}) ; et {@code endTime} doit être après {@code startTime}.
 *
 * @param courseLabel  intitulé du cours, obligatoire
 * @param formateurRef intervenant (people.staff.id), optionnel
 * @param dayOfWeek    jour récurrent (exclusif avec sessionDate)
 * @param sessionDate  date ponctuelle (exclusif avec dayOfWeek)
 * @param startTime    heure de début, obligatoire
 * @param endTime      heure de fin, obligatoire
 * @param room         salle ou lien visio, optionnel
 */
public record CreneauCreationDto(
        @NotBlank(message = "L'intitulé du cours est obligatoire.")
        String courseLabel,

        UUID formateurRef,

        JourSemaine dayOfWeek,

        LocalDate sessionDate,

        @NotNull(message = "L'heure de début est obligatoire.")
        LocalTime startTime,

        @NotNull(message = "L'heure de fin est obligatoire.")
        LocalTime endTime,

        String room
) {
}
