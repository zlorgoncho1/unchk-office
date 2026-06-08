package sn.unchk.office.communication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.communication.messaging.payload.NotificationEvent;
import sn.unchk.office.communication.messaging.producer.EnregistreurEvenement;
import sn.unchk.office.communication.projection.IdentityUserRo;
import sn.unchk.office.communication.repository.IdentityUserRoRepository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Résolution des destinataires et production des notifications.
 * <p>
 * Résout les destinataires UNIQUEMENT à partir des read-models locaux (zéro appel REST),
 * conformément à l'architecture CQRS : on lit {@code identity_user_ro} (par rôle) pour les
 * diffusions par visibilité, et on accepte des destinataires explicites (participants d'une
 * réunion). Pour chaque destinataire, un événement est mis en file sur le topic
 * {@code notifications} (clé = recipientId) via l'Outbox.
 */
@Service
public class ServiceNotification {

    private static final Logger log = LoggerFactory.getLogger(ServiceNotification.class);

    private final IdentityUserRoRepository utilisateursRo;
    private final EnregistreurEvenement enregistreur;

    public ServiceNotification(IdentityUserRoRepository utilisateursRo,
                               EnregistreurEvenement enregistreur) {
        this.utilisateursRo = utilisateursRo;
        this.enregistreur = enregistreur;
    }

    /**
     * Notifie tous les utilisateurs actifs possédant au moins un des rôles de visibilité.
     * Utilisé pour les comptes rendus et circulaires (diffusion par rôle).
     */
    @Transactional
    public void notifierParRoles(Collection<String> roles, String kind, String titre,
                                 String message, String targetService, UUID targetRef) {
        if (roles == null || roles.isEmpty()) {
            log.debug("Aucun rôle de visibilité : pas de destinataire à notifier (cible={})", targetRef);
            return;
        }
        List<IdentityUserRo> destinataires =
                utilisateursRo.trouverActifsParRoles(roles.toArray(new String[0]));
        Set<UUID> ids = new LinkedHashSet<>();
        for (IdentityUserRo u : destinataires) {
            ids.add(u.getId());
        }
        emettre(ids, kind, titre, message, targetService, targetRef);
    }

    /**
     * Notifie une liste explicite de destinataires (ex : participants d'une réunion).
     */
    @Transactional
    public void notifierDestinataires(Collection<UUID> recipientIds, String kind, String titre,
                                      String message, String targetService, UUID targetRef) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        emettre(new LinkedHashSet<>(recipientIds), kind, titre, message, targetService, targetRef);
    }

    /** Met en file un événement notification par destinataire (dédupliqué). */
    private void emettre(Set<UUID> recipientIds, String kind, String titre, String message,
                         String targetService, UUID targetRef) {
        for (UUID recipientId : recipientIds) {
            NotificationEvent event = new NotificationEvent(
                    recipientId, kind, titre, message, targetService, targetRef);
            // Clé de partition = recipientId (ordre par destinataire).
            enregistreur.enregistrer("Notification", recipientId, Topics.NOTIFICATIONS,
                    "NotificationCreee", event);
        }
        log.debug("{} notification(s) mise(s) en file (kind={}, cible={})",
                recipientIds.size(), kind, targetRef);
    }
}
