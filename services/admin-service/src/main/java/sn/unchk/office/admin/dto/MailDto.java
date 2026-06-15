package sn.unchk.office.admin.dto;

import sn.unchk.office.admin.domain.MailDirection;
import sn.unchk.office.admin.domain.MailStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue de lecture d'un courrier (réponse API).
 *
 * @param id            identifiant
 * @param reference     référence / numéro
 * @param direction     sens (arrivé / départ)
 * @param subject       objet
 * @param correspondent correspondant
 * @param mailDate      date du courrier
 * @param registeredAt  date d'enregistrement
 * @param status        statut de traitement
 * @param assignedTo    agent en charge (→ people.staff.id)
 * @param documentRef   pièce scannée (→ document.documents.id)
 * @param notes         annotations
 * @param createdAt     date de création
 * @param updatedAt     date de dernière modification
 */
public record MailDto(
        UUID id,
        String reference,
        MailDirection direction,
        String subject,
        String correspondent,
        LocalDate mailDate,
        LocalDate registeredAt,
        MailStatus status,
        UUID assignedTo,
        UUID documentRef,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
