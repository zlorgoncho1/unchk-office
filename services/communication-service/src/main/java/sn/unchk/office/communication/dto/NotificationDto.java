package sn.unchk.office.communication.dto;

import sn.unchk.office.communication.domain.Notification;
import sn.unchk.office.communication.domain.NotificationKind;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation d'une notification renvoyée au client (badge + historique).
 */
public record NotificationDto(
        UUID id,
        UUID recipientId,
        NotificationKind kind,
        String title,
        String message,
        String targetService,
        UUID targetRef,
        boolean read,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationDto de(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getRecipientId(),
                n.getKind(),
                n.getTitle(),
                n.getMessage(),
                n.getTargetService(),
                n.getTargetRef(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
