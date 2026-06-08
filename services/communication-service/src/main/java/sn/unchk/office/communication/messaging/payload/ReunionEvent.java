package sn.unchk.office.communication.messaging.payload;

import sn.unchk.office.communication.domain.Reunion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * État d'une réunion transporté sur le topic {@code communication.reunions}
 * (event-carried state transfer). Sérialisé en JSON dans le payload de l'enveloppe DomainEvent.
 *
 * @param id             identifiant de la réunion (= clé de partition)
 * @param title          titre
 * @param type           type de réunion
 * @param startsAt       début
 * @param endsAt         fin
 * @param status         statut
 * @param organizerId    organisateur
 * @param formationRef   formation liée
 * @param participantIds destinataires de la convocation (UUID)
 */
public record ReunionEvent(
        UUID id,
        String title,
        String type,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        UUID organizerId,
        UUID formationRef,
        List<UUID> participantIds
) {

    /**
     * Projette l'entité réunion vers son événement, avec la liste des participants.
     */
    public static ReunionEvent de(Reunion reunion, List<UUID> participantIds) {
        return new ReunionEvent(
                reunion.getId(),
                reunion.getTitle(),
                reunion.getType().name(),
                reunion.getStartsAt(),
                reunion.getEndsAt(),
                reunion.getStatus().name(),
                reunion.getOrganizerId(),
                reunion.getFormationRef(),
                participantIds);
    }
}
