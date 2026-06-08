package sn.unchk.office.communication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.communication.domain.MeetingType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Requête de planification d'une réunion.
 * <p>
 * DTO dédié (jamais l'entité JPA en corps de requête) : les champs système
 * ({@code id}, {@code createdBy}, {@code status}...) ne sont jamais liés depuis le client
 * (anti sur-affectation / mass assignment).
 *
 * @param title        titre (obligatoire)
 * @param type         type de réunion (obligatoire)
 * @param description  description libre
 * @param location     salle ou lien visio
 * @param startsAt     début (obligatoire)
 * @param endsAt       fin (optionnelle, doit être ≥ début côté service)
 * @param organizerId  organisateur (réf. people.staff.id)
 * @param formationRef formation liée (optionnelle)
 * @param participants participants à inviter
 */
public record ReunionCreationRequest(
        @NotBlank @Size(max = 500) String title,
        @NotNull MeetingType type,
        @Size(max = 5000) String description,
        @Size(max = 1000) String location,
        @NotNull OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        @NotNull UUID organizerId,
        UUID formationRef,
        @Valid List<ParticipantDto> participants
) {
}
