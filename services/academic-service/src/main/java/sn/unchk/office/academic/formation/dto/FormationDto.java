package sn.unchk.office.academic.formation.dto;

import sn.unchk.office.academic.formation.Financement;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.academic.formation.NiveauFormation;
import sn.unchk.office.academic.formation.TypeFormation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation d'une formation renvoyée par l'API (vue lecture).
 *
 * @param id             identifiant
 * @param code           code unique
 * @param label          intitulé
 * @param level          niveau
 * @param kind           type
 * @param funding        financement
 * @param startDate      date de début
 * @param endDate        date de fin
 * @param trainedMale    formés (hommes)
 * @param trainedFemale  formés (femmes)
 * @param responsibleRef responsable (people.staff.id)
 * @param active         active
 * @param createdAt      création
 * @param updatedAt      dernière modification
 */
public record FormationDto(
        UUID id,
        String code,
        String label,
        NiveauFormation level,
        TypeFormation kind,
        Financement funding,
        LocalDate startDate,
        LocalDate endDate,
        int trainedMale,
        int trainedFemale,
        UUID responsibleRef,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    /** Construit le DTO de sortie à partir de l'entité persistée. */
    public static FormationDto de(Formation f) {
        return new FormationDto(
                f.getId(),
                f.getCode(),
                f.getLabel(),
                f.getLevel(),
                f.getKind(),
                f.getFunding(),
                f.getStartDate(),
                f.getEndDate(),
                f.getTrainedMale(),
                f.getTrainedFemale(),
                f.getResponsibleRef(),
                f.isActive(),
                f.getCreatedAt(),
                f.getUpdatedAt());
    }
}
