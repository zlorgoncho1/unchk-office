package sn.unchk.office.insertion.dto;

import sn.unchk.office.insertion.domain.ContactLog;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation d'une entrée du registre de contact renvoyée au client.
 */
public record ContactLogResponse(
        UUID id,
        UUID studentRef,
        LocalDate contactedAt,
        String channel,
        String notes,
        UUID agentRef
) {

    public static ContactLogResponse depuis(ContactLog c) {
        return new ContactLogResponse(
                c.getId(),
                c.getStudentRef(),
                c.getContactedAt(),
                c.getChannel(),
                c.getNotes(),
                c.getAgentRef());
    }
}
