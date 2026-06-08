package sn.unchk.office.document.dto;

import sn.unchk.office.document.domain.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Représentation des métadonnées d'un document renvoyée par l'API.
 * <p>
 * Ne contient jamais le binaire : le téléchargement passe par une URL présignée MinIO
 * obtenue via un endpoint dédié et contrôlé par OPA (anti-IDOR).
 */
public record DocumentReponse(
        UUID id,
        String title,
        String category,
        String description,
        String mimeType,
        long sizeBytes,
        String checksumSha256,
        UUID ownerId,
        boolean archived,
        String sourceService,
        UUID sourceRef,
        List<String> visibility,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Construit la réponse à partir de l'entité et de sa liste de rôles visibles.
     */
    public static DocumentReponse depuis(Document document, List<String> visibility) {
        return new DocumentReponse(
                document.getId(),
                document.getTitle(),
                document.getCategory() != null ? document.getCategory().code() : null,
                document.getDescription(),
                document.getMimeType(),
                document.getSizeBytes(),
                document.getChecksumSha256(),
                document.getOwnerId(),
                document.isArchived(),
                document.getSourceService(),
                document.getSourceRef(),
                visibility,
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
