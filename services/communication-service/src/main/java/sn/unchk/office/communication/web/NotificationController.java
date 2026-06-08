package sn.unchk.office.communication.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.communication.dto.NotificationDto;
import sn.unchk.office.communication.service.ServiceConsultationNotification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST des notifications de l'utilisateur courant, sous {@code /api/communication/notifications}.
 * <p>
 * Anti-IDOR fort : le destinataire est TOUJOURS l'utilisateur authentifié, résolu côté serveur
 * ({@code subject.id}). Aucun identifiant d'utilisateur n'est accepté du client ; un utilisateur
 * ne peut donc consulter ou modifier que SES propres notifications.
 */
@RestController
@RequestMapping("/api/communication/notifications")
public class NotificationController {

    private final ServiceConsultationNotification service;

    public NotificationController(ServiceConsultationNotification service) {
        this.service = service;
    }

    /** Historique des notifications de l'utilisateur courant. */
    @GetMapping
    public List<NotificationDto> historique() {
        return service.historique(UtilisateurCourant.id());
    }

    /** Nombre de notifications non lues (badge cloche). */
    @GetMapping("/non-lues/count")
    public Map<String, Long> compterNonLues() {
        return Map.of("count", service.compterNonLues(UtilisateurCourant.id()));
    }

    /** Marque une notification de l'utilisateur courant comme lue. */
    @PatchMapping("/{id}/lue")
    public NotificationDto marquerLue(@PathVariable UUID id) {
        // On borne au destinataire courant : une notification d'autrui renvoie 404.
        return service.marquerLue(id, UtilisateurCourant.id());
    }
}
