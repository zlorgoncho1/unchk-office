package sn.unchk.office.academic.formation.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile « tombstone logique » publiée lors d'une suppression de formation
 * (eventType = {@code Deleted}) sur {@code academic.formations}.
 *
 * @param id        identifiant de la formation supprimée
 * @param deletedAt instant de la suppression
 * @param deletedBy auteur de la suppression (identity.users.id)
 */
public record FormationTombstonePayload(
        UUID id,
        Instant deletedAt,
        UUID deletedBy
) {
}
