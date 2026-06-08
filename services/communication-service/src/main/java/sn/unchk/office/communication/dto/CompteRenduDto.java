package sn.unchk.office.communication.dto;

import sn.unchk.office.communication.domain.CompteRendu;
import sn.unchk.office.communication.domain.MeetingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Représentation d'un compte rendu renvoyée au client.
 */
public record CompteRenduDto(
        UUID id,
        UUID reunionId,
        String title,
        MeetingType type,
        String body,
        UUID documentRef,
        LocalDate meetingDate,
        UUID authorId,
        String authorName,
        boolean published,
        Instant publishedAt,
        Set<String> visibility
) {

    /**
     * Construit le DTO à partir de l'entité et du nom d'auteur (read-model).
     */
    public static CompteRenduDto de(CompteRendu cr, String authorName) {
        return new CompteRenduDto(
                cr.getId(),
                cr.getReunionId(),
                cr.getTitle(),
                cr.getType(),
                cr.getBody(),
                cr.getDocumentRef(),
                cr.getMeetingDate(),
                cr.getAuthorId(),
                authorName,
                cr.isPublished(),
                cr.getPublishedAt(),
                new TreeSet<>(cr.getVisibility()));
    }
}
