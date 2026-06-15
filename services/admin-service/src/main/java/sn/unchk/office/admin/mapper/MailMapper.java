package sn.unchk.office.admin.mapper;

import org.springframework.stereotype.Component;
import sn.unchk.office.admin.domain.Mail;
import sn.unchk.office.admin.dto.MailDto;

/**
 * Transforme l'entité {@link Mail} en DTO de réponse (mapping explicite, jamais l'entité exposée).
 */
@Component
public class MailMapper {

    /** Construit la vue de lecture d'un courrier. */
    public MailDto versDto(Mail mail) {
        return new MailDto(
                mail.getId(),
                mail.getReference(),
                mail.getDirection(),
                mail.getSubject(),
                mail.getCorrespondent(),
                mail.getMailDate(),
                mail.getRegisteredAt(),
                mail.getStatus(),
                mail.getAssignedTo(),
                mail.getDocumentRef(),
                mail.getNotes(),
                mail.getCreatedAt(),
                mail.getUpdatedAt());
    }
}
