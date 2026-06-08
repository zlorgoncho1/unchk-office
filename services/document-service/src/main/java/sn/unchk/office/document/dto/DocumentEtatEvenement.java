package sn.unchk.office.document.dto;

import sn.unchk.office.document.domain.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * État de l'agrégat « document » publié sur le topic {@code document.documents}.
 * <p>
 * C'est la charge utile (payload) de l'enveloppe {@code DomainEvent} : event-carried
 * state transfer. Les consommateurs (communication pour les circulaires, admin, OPA pour
 * l'ABAC) reconstruisent leurs read-models à partir de cet état. On n'y met JAMAIS le
 * binaire : seulement les métadonnées + la clé MinIO + la visibilité.
 *
 * @param id            identifiant du document (= clé de partition)
 * @param title         titre
 * @param category      catégorie (code base)
 * @param bucket        bucket MinIO
 * @param objectKey     clé objet MinIO
 * @param mimeType      type MIME
 * @param sizeBytes     taille
 * @param ownerId       propriétaire (ABAC)
 * @param visibility    rôles autorisés (ABAC visibility[])
 * @param archived      état d'archivage
 * @param sourceService service d'origine
 * @param sourceRef     identifiant métier d'origine
 * @param occurredAt    instant de l'état
 */
public record DocumentEtatEvenement(
        UUID id,
        String title,
        String category,
        String bucket,
        String objectKey,
        String mimeType,
        long sizeBytes,
        UUID ownerId,
        List<String> visibility,
        boolean archived,
        String sourceService,
        UUID sourceRef,
        Instant occurredAt
) {

    /** Construit le payload d'état à partir de l'entité et de sa visibilité. */
    public static DocumentEtatEvenement depuis(Document document, List<String> visibility) {
        return new DocumentEtatEvenement(
                document.getId(),
                document.getTitle(),
                document.getCategory() != null ? document.getCategory().code() : null,
                document.getBucket(),
                document.getObjectKey(),
                document.getMimeType(),
                document.getSizeBytes(),
                document.getOwnerId(),
                visibility,
                document.isArchived(),
                document.getSourceService(),
                document.getSourceRef(),
                Instant.now());
    }
}
