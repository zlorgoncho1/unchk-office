package sn.unchk.office.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.communication.domain.MeetingType;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Requête de rédaction d'un compte rendu (état initial : brouillon, non publié).
 *
 * @param reunionId    réunion source (optionnelle)
 * @param title        titre (obligatoire)
 * @param type         type de réunion (obligatoire)
 * @param body         contenu rédigé
 * @param documentRef  document PDF archivé (optionnel)
 * @param meetingDate  date de la réunion (obligatoire)
 * @param authorId     rédacteur (réf. people.staff.id)
 * @param visibility   rôles autorisés à consulter (visibilité ABAC) ; au moins un rôle
 */
public record CompteRenduCreationRequest(
        UUID reunionId,
        @NotBlank @Size(max = 500) String title,
        @NotNull MeetingType type,
        @Size(max = 50000) String body,
        UUID documentRef,
        @NotNull LocalDate meetingDate,
        @NotNull UUID authorId,
        @NotNull @Size(min = 1, max = 5) Set<@NotBlank String> visibility
) {
}
