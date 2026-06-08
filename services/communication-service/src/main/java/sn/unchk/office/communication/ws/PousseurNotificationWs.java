package sn.unchk.office.communication.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.communication.dto.NotificationDto;

/**
 * Pousse une notification vers la session WebSocket du destinataire (frame STOMP).
 * <p>
 * Le message est adressé à la destination utilisateur {@code /user/queue/notifications} ;
 * Spring le route vers la (ou les) session(s) dont le {@code Principal} correspond au
 * destinataire. Si aucune session n'est active, l'envoi est simplement sans effet :
 * la notification reste en base (badge + historique) et sera vue au prochain chargement.
 */
@Component
public class PousseurNotificationWs {

    private static final Logger log = LoggerFactory.getLogger(PousseurNotificationWs.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PousseurNotificationWs(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envoie la notification au destinataire identifié par son UUID (nom de Principal).
     *
     * @param notification notification à pousser
     * @return {@code true} si l'envoi a été tenté sans erreur
     */
    public boolean pousser(NotificationDto notification) {
        String destinataire = notification.recipientId().toString();
        try {
            messagingTemplate.convertAndSendToUser(
                    destinataire,
                    ConfigurationWebSocket.DESTINATION_NOTIFICATIONS,
                    notification);
            log.debug("Notification poussée (tentative WS) destinataire={} id={}",
                    destinataire, notification.id());
            return true;
        } catch (Exception ex) {
            // Le push n'est pas critique : on journalise sans faire échouer la consommation.
            log.warn("Échec du push WebSocket destinataire={} : {}", destinataire, ex.getMessage());
            return false;
        }
    }
}
