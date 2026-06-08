package sn.unchk.office.communication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.communication.domain.Notification;
import sn.unchk.office.communication.dto.NotificationDto;
import sn.unchk.office.communication.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consultation et gestion des notifications de l'utilisateur courant.
 * <p>
 * Tout est borné au destinataire courant ({@code recipientId} = subject.id résolu côté
 * serveur) : un utilisateur ne voit et ne marque que SES notifications (anti-IDOR).
 */
@Service
public class ServiceConsultationNotification {

    private final NotificationRepository notificationRepository;

    public ServiceConsultationNotification(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Historique des notifications du destinataire. */
    @Transactional(readOnly = true)
    public List<NotificationDto> historique(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(NotificationDto::de)
                .toList();
    }

    /** Nombre de notifications non lues (badge cloche). */
    @Transactional(readOnly = true)
    public long compterNonLues(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    /**
     * Marque une notification comme lue, à condition qu'elle appartienne au destinataire.
     *
     * @throws RessourceIntrouvableException si la notification n'existe pas pour ce destinataire
     */
    @Transactional
    public NotificationDto marquerLue(UUID id, UUID recipientId) {
        Notification n = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Notification introuvable."));
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
            n = notificationRepository.save(n);
        }
        return NotificationDto.de(n);
    }
}
