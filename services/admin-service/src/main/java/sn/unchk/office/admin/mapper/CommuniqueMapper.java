package sn.unchk.office.admin.mapper;

import org.springframework.stereotype.Component;
import sn.unchk.office.admin.domain.AdminCommunique;
import sn.unchk.office.admin.dto.CommuniqueDto;
import sn.unchk.office.admin.messaging.CommuniqueEventPayload;
import sn.unchk.office.admin.service.Audiences;

import java.util.List;

/**
 * Transforme l'entité {@link AdminCommunique} en DTO de réponse et en charge utile d'événement.
 */
@Component
public class CommuniqueMapper {

    /** Construit la vue de lecture d'un communiqué (audience déduite des rôles ciblés). */
    public CommuniqueDto versDto(AdminCommunique c) {
        List<String> roles = List.copyOf(c.getTargets());
        return new CommuniqueDto(
                c.getId(),
                c.getKind(),
                c.getReference(),
                c.getTitle(),
                c.getBody(),
                c.getDocumentRef(),
                c.getIssueDate(),
                c.isPublished(),
                c.getPublishedAt(),
                Audiences.audiencePour(c.getTargets()),
                roles,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    /** Construit la charge utile Kafka pour la notification (à la publication). */
    public CommuniqueEventPayload versPayload(AdminCommunique c) {
        return new CommuniqueEventPayload(
                c.getId(),
                c.getKind().name(),
                c.getTitle(),
                List.copyOf(c.getTargets()),
                c.isPublished());
    }
}
