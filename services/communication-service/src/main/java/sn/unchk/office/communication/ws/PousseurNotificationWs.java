package sn.unchk.office.communication.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sn.unchk.office.communication.dto.NotificationDto;

/**
 * Pousse une notification vers la (les) session(s) WebSocket du destinataire.
 * <p>
 * Si aucune session n'est ouverte, l'envoi est simplement sans effet : la notification reste
 * en base (badge + historique) et sera vue au prochain chargement.
 */
@Component
public class PousseurNotificationWs {

    private static final Logger log = LoggerFactory.getLogger(PousseurNotificationWs.class);

    private final GestionnaireNotificationsWs gestionnaire;

    public PousseurNotificationWs(GestionnaireNotificationsWs gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    /**
     * Envoie la notification au destinataire identifié par son UUID.
     *
     * @return {@code true} si au moins une session a reçu le message.
     */
    public boolean pousser(NotificationDto notification) {
        String destinataire = notification.recipientId().toString();
        boolean pousse = gestionnaire.pousser(destinataire, notification);
        if (pousse) {
            log.debug("Notification poussée (WS) destinataire={} id={}", destinataire, notification.id());
        }
        return pousse;
    }
}
