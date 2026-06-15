package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotNull;
import sn.unchk.office.admin.domain.MailStatus;

/**
 * Changement de statut d'un courrier (cycle de vie du traitement).
 *
 * @param status nouveau statut (reçu, en traitement, traité, archivé, clos)
 */
public record ChangementStatutMailDto(
        @NotNull(message = "Le statut est obligatoire.")
        MailStatus status
) {
}
