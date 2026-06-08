package sn.unchk.office.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.document.domain.IdentityUserRo;
import sn.unchk.office.document.domain.ProcessedEvent;
import sn.unchk.office.document.repository.IdentityUserRoRepository;
import sn.unchk.office.document.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Applique les événements {@code identity.users} sur le read-model local des comptes.
 * <p>
 * Garantit l'idempotence : un eventId déjà traité est ignoré. Les événements de type
 * {@code Deleted} (tombstone logique) suppriment l'entrée correspondante.
 */
@Service
public class ProjectionUtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(ProjectionUtilisateurService.class);

    private final IdentityUserRoRepository utilisateurs;
    private final ProcessedEventRepository evenementsTraites;

    public ProjectionUtilisateurService(IdentityUserRoRepository utilisateurs,
                                        ProcessedEventRepository evenementsTraites) {
        this.utilisateurs = utilisateurs;
        this.evenementsTraites = evenementsTraites;
    }

    /**
     * Applique un événement à la projection, en une transaction et de façon idempotente.
     */
    @Transactional
    public void appliquer(DomainEvent<Map<String, Object>> evenement) {
        UUID eventId = evenement.eventId();
        if (eventId != null && evenementsTraites.existsById(eventId)) {
            // Déjà traité : on ignore (rejeu Kafka possible).
            log.debug("Événement identity.users déjà traité, ignoré : {}", eventId);
            return;
        }

        Map<String, Object> payload = evenement.payload();
        if (payload != null) {
            UUID id = lireUuid(payload.get("id"));
            if (id != null) {
                String type = evenement.eventType();
                if (type != null && type.toLowerCase().contains("deleted")) {
                    // Tombstone logique : on retire l'entrée de la projection.
                    utilisateurs.deleteById(id);
                } else {
                    String roles = lireRoles(payload.get("roles"));
                    String status = payload.get("status") != null ? payload.get("status").toString() : null;
                    utilisateurs.save(new IdentityUserRo(id, roles, status, Instant.now()));
                }
            }
        }

        if (eventId != null) {
            evenementsTraites.save(new ProcessedEvent(eventId));
        }
    }

    /** Convertit en UUID en tolérant null. */
    private UUID lireUuid(Object valeur) {
        return valeur == null ? null : UUID.fromString(valeur.toString());
    }

    /** Normalise les rôles (liste ou chaîne) en une chaîne séparée par des virgules. */
    @SuppressWarnings("unchecked")
    private String lireRoles(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof Collection<?> collection) {
            return String.join(",", ((Collection<Object>) collection).stream().map(Object::toString).toList());
        }
        return valeur.toString();
    }

    /** Utilitaire : transforme une chaîne « r1,r2 » en liste de rôles. */
    public static List<String> rolesEnListe(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return List.of(roles.split("\\s*,\\s*"));
    }
}
