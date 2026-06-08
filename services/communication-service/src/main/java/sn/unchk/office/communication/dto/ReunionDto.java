package sn.unchk.office.communication.dto;

import sn.unchk.office.communication.domain.MeetingStatus;
import sn.unchk.office.communication.domain.MeetingType;
import sn.unchk.office.communication.domain.Reunion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Représentation d'une réunion renvoyée au client.
 */
public record ReunionDto(
        UUID id,
        String title,
        MeetingType type,
        String description,
        String location,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        MeetingStatus status,
        UUID organizerId,
        String organizerName,
        UUID formationRef,
        List<ParticipantDto> participants
) {

    /**
     * Construit le DTO à partir de l'entité, du nom d'organisateur (read-model) et des participants.
     */
    public static ReunionDto de(Reunion reunion, String organizerName, List<ParticipantDto> participants) {
        return new ReunionDto(
                reunion.getId(),
                reunion.getTitle(),
                reunion.getType(),
                reunion.getDescription(),
                reunion.getLocation(),
                reunion.getStartsAt(),
                reunion.getEndsAt(),
                reunion.getStatus(),
                reunion.getOrganizerId(),
                organizerName,
                reunion.getFormationRef(),
                participants);
    }
}
