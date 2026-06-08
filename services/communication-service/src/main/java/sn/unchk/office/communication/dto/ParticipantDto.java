package sn.unchk.office.communication.dto;

import jakarta.validation.constraints.NotNull;
import sn.unchk.office.communication.domain.PersonKind;

import java.util.UUID;

/**
 * Participant d'une réunion (DTO d'entrée/sortie).
 *
 * @param personRef  identifiant (UUID) de la personne (staff ou étudiant)
 * @param personKind nature de la personne (staff / student)
 * @param isPresent  émargement (peut être null à la planification)
 */
public record ParticipantDto(
        @NotNull UUID personRef,
        @NotNull PersonKind personKind,
        Boolean isPresent
) {
}
