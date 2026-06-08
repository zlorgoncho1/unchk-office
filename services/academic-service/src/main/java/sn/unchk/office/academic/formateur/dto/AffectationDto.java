package sn.unchk.office.academic.formateur.dto;

import sn.unchk.office.academic.formateur.AffectationFormateur;

import java.time.Instant;
import java.util.UUID;

/**
 * Affectation d'un formateur renvoyée par l'API, enrichie du nom du formateur
 * (résolu depuis le read-model local {@code academic_formateur_ro}, sans appel REST).
 *
 * @param formationId    formation
 * @param formateurRef   formateur (people.staff.id)
 * @param formateurNom   nom du formateur (depuis la projection locale, peut être {@code null}
 *                       si le formateur n'est pas encore projeté)
 * @param module         matière enseignée
 * @param assignedAt     date d'affectation
 */
public record AffectationDto(
        UUID formationId,
        UUID formateurRef,
        String formateurNom,
        String module,
        Instant assignedAt
) {

    /** Construit le DTO à partir de l'affectation et du nom résolu localement. */
    public static AffectationDto de(AffectationFormateur a, String formateurNom) {
        return new AffectationDto(
                a.getFormationId(),
                a.getFormateurRef(),
                formateurNom,
                a.getModule(),
                a.getAssignedAt());
    }
}
