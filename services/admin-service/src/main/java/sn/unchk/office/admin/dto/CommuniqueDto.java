package sn.unchk.office.admin.dto;

import sn.unchk.office.admin.domain.AdminDocKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vue de lecture d'un communiqué (réponse API).
 *
 * @param id          identifiant
 * @param kind        nature (note de service / circulaire)
 * @param reference   référence / numéro
 * @param title       titre / objet
 * @param body        corps
 * @param documentRef pièce jointe (→ document.documents.id)
 * @param issueDate   date d'émission
 * @param published   indicateur de publication
 * @param publishedAt horodatage de publication
 * @param audience    audience (préréglage déduit des rôles)
 * @param targets     rôles destinataires
 * @param createdAt   date de création
 * @param updatedAt   date de dernière modification
 */
public record CommuniqueDto(
        UUID id,
        AdminDocKind kind,
        String reference,
        String title,
        String body,
        UUID documentRef,
        LocalDate issueDate,
        boolean published,
        Instant publishedAt,
        String audience,
        List<String> targets,
        Instant createdAt,
        Instant updatedAt
) {
}
