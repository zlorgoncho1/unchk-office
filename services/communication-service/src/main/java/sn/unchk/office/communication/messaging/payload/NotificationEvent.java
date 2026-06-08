package sn.unchk.office.communication.messaging.payload;

import java.util.UUID;

/**
 * Notification destinée à un utilisateur, transportée sur le topic {@code notifications}
 * (clé de partition = {@code recipientId}). Consommée par le service lui-même pour
 * persister la notification et la pousser via WebSocket.
 *
 * @param recipientId   destinataire (= clé de partition)
 * @param kind          catégorie (compte_rendu, circulaire, reunion...)
 * @param title         titre
 * @param message       message
 * @param targetService service cible pour le deep-link
 * @param targetRef     ressource cible pour le deep-link
 */
public record NotificationEvent(
        UUID recipientId,
        String kind,
        String title,
        String message,
        String targetService,
        UUID targetRef
) {
}
