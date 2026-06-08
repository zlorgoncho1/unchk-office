package sn.unchk.office.communication.messaging.payload;

import sn.unchk.office.communication.domain.CompteRendu;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * État d'un compte rendu transporté sur le topic {@code communication.comptesrendus}.
 *
 * @param id          identifiant du compte rendu (= clé de partition)
 * @param reunionId   réunion source
 * @param title       titre
 * @param type        type de réunion
 * @param meetingDate date de la réunion
 * @param authorId    rédacteur
 * @param ownerId     propriétaire (créateur), pour l'ABAC
 * @param published   indicateur de publication
 * @param publishedAt date de publication
 * @param visibility  rôles autorisés à consulter (résolution des destinataires)
 */
public record CompteRenduEvent(
        UUID id,
        UUID reunionId,
        String title,
        String type,
        LocalDate meetingDate,
        UUID authorId,
        UUID ownerId,
        boolean published,
        Instant publishedAt,
        List<String> visibility
) {

    public static CompteRenduEvent de(CompteRendu cr) {
        return new CompteRenduEvent(
                cr.getId(),
                cr.getReunionId(),
                cr.getTitle(),
                cr.getType().name(),
                cr.getMeetingDate(),
                cr.getAuthorId(),
                cr.getCreatedBy(),
                cr.isPublished(),
                cr.getPublishedAt(),
                List.copyOf(cr.getVisibility()));
    }
}
