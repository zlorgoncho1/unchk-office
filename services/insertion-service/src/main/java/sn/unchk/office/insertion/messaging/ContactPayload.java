package sn.unchk.office.insertion.messaging;

import sn.unchk.office.insertion.domain.ContactLog;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Charge utile d'un événement « contact de suivi » publié sur {@code insertion.events}.
 */
public record ContactPayload(
        UUID id,
        UUID studentRef,
        LocalDate contactedAt,
        String channel
) {

    public static ContactPayload depuis(ContactLog c) {
        return new ContactPayload(
                c.getId(),
                c.getStudentRef(),
                c.getContactedAt(),
                c.getChannel());
    }
}
