package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux notifications (badge + historique).
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Historique des notifications d'un destinataire, du plus récent au plus ancien. */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    /** Notifications non lues d'un destinataire. */
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId);

    /** Compte des notifications non lues (badge cloche). */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /** Notification par identifiant et destinataire (anti-IDOR : on borne au propriétaire). */
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);
}
