package sn.unchk.office.academic.emploidutemps.dto;

import sn.unchk.office.academic.emploidutemps.Creneau;
import sn.unchk.office.academic.emploidutemps.JourSemaine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Créneau d'emploi du temps renvoyé par l'API, enrichi du nom de l'intervenant
 * (résolu depuis le read-model local, sans appel REST).
 *
 * @param id           identifiant du créneau
 * @param formationId  formation
 * @param courseLabel  intitulé du cours
 * @param formateurRef intervenant (people.staff.id)
 * @param formateurNom nom de l'intervenant (depuis la projection locale, peut être {@code null})
 * @param dayOfWeek    jour récurrent
 * @param sessionDate  date ponctuelle
 * @param startTime    heure de début
 * @param endTime      heure de fin
 * @param room         salle ou lien visio
 */
public record CreneauDto(
        UUID id,
        UUID formationId,
        String courseLabel,
        UUID formateurRef,
        String formateurNom,
        JourSemaine dayOfWeek,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String room
) {

    /** Construit le DTO à partir du créneau et du nom d'intervenant résolu localement. */
    public static CreneauDto de(Creneau c, String formateurNom) {
        return new CreneauDto(
                c.getId(),
                c.getFormationId(),
                c.getCourseLabel(),
                c.getFormateurRef(),
                formateurNom,
                c.getDayOfWeek(),
                c.getSessionDate(),
                c.getStartTime(),
                c.getEndTime(),
                c.getRoom());
    }
}
