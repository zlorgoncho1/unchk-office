package sn.unchk.office.communication.messaging.consumer.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Vue partielle d'un communiqué administratif reçu sur {@code admin.communiques}.
 * <p>
 * Sert uniquement à déclencher des notifications : à la publication d'une note de service ou
 * d'une circulaire, on notifie les rôles ciblés ({@code targets}). Champs inconnus ignorés.
 *
 * @param id        identifiant du communiqué
 * @param kind      nature (note_service / circulaire) — devient le {@code NotificationKind}
 * @param title     titre / objet
 * @param targets   rôles destinataires
 * @param published indicateur de publication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommuniqueProjete(
        UUID id,
        String kind,
        String title,
        List<String> targets,
        Boolean published
) {
}
