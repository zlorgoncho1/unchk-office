package sn.unchk.office.admin.messaging;

import java.util.List;
import java.util.UUID;

/**
 * Charge utile publiée sur {@code admin.communiques} lors de la publication d'un communiqué.
 * <p>
 * Sert au communication-service à déclencher les notifications par rôle (« note de service » /
 * « circulaire »). On ne transporte que ce qui est nécessaire à la notification.
 *
 * @param id        identifiant du communiqué
 * @param kind      nature (note_service / circulaire) — sert de {@code NotificationKind}
 * @param title     titre / objet
 * @param targets   rôles destinataires
 * @param published indicateur de publication
 */
public record CommuniqueEventPayload(
        UUID id,
        String kind,
        String title,
        List<String> targets,
        boolean published
) {
}
