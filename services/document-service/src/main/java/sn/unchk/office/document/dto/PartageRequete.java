package sn.unchk.office.document.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Partage nominatif d'un document avec un utilisateur précis.
 *
 * @param userId  identifiant de l'utilisateur destinataire (obligatoire)
 * @param canEdit autorise-t-on l'édition (par défaut : lecture seule)
 */
public record PartageRequete(

        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire.")
        UUID userId,

        boolean canEdit
) {
}
