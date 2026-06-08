package sn.unchk.office.insertion.messaging;

import sn.unchk.office.insertion.domain.Internship;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Charge utile d'un événement « stage » publié sur {@code insertion.events}.
 * <p>
 * La clé de partition du topic étant {@code studentId}, l'identifiant de l'étudiant
 * est toujours présent pour permettre le routage et le suivi côté consommateurs.
 */
public record StagePayload(
        UUID id,
        UUID studentRef,
        UUID partnerId,
        String title,
        String status,
        LocalDate startDate,
        LocalDate endDate
) {

    public static StagePayload depuis(Internship s) {
        return new StagePayload(
                s.getId(),
                s.getStudentRef(),
                s.getPartnerId(),
                s.getTitle(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getStartDate(),
                s.getEndDate());
    }
}
