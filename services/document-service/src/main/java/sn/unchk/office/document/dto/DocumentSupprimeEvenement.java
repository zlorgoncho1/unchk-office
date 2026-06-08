package sn.unchk.office.document.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload « tombstone » logique émis lors d'un événement {@code Deleted} sur
 * {@code document.documents} (conforme à l'enveloppe d'événement de l'architecture).
 *
 * @param id        identifiant du document supprimé
 * @param deletedAt instant de suppression
 * @param deletedBy identifiant de l'utilisateur ayant supprimé
 */
public record DocumentSupprimeEvenement(
        UUID id,
        Instant deletedAt,
        UUID deletedBy
) {
}
