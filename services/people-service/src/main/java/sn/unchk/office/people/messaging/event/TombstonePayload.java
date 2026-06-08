package sn.unchk.office.people.messaging.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile d'un evenement de suppression logique ({@code Deleted}).
 * <p>
 * Conforme au tombstone logique decrit dans docs/architecture.md :
 * {@code { "id": "<uuid>", "deletedAt": "<iso>", "deletedBy": "<uuid-user>" }}.
 * Les consommateurs retirent la cle de leur read-model.
 *
 * @param id        identifiant de l'agregat supprime
 * @param deletedAt horodatage de la suppression
 * @param deletedBy auteur de la suppression (identity.users.id)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TombstonePayload(
        UUID id,
        Instant deletedAt,
        UUID deletedBy
) {
}
