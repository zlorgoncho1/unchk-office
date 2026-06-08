package sn.unchk.office.academic.formateur.dto;

import sn.unchk.office.academic.formateur.FormateurRo;

import java.util.UUID;

/**
 * Vue d'un formateur exposée par l'API, issue du read-model local {@code academic_formateur_ro}
 * (projection alimentée par people.staff, sans appel REST vers people-service).
 *
 * @param id         identifiant (people.staff.id)
 * @param fullName   nom complet
 * @param kind       type de personnel
 * @param speciality spécialité
 * @param active     en activité
 */
public record FormateurDto(
        UUID id,
        String fullName,
        String kind,
        String speciality,
        boolean active
) {

    /** Construit le DTO à partir de l'entrée de projection. */
    public static FormateurDto de(FormateurRo f) {
        return new FormateurDto(
                f.getId(),
                f.getFullName(),
                f.getKind(),
                f.getSpeciality(),
                f.isActive());
    }
}
