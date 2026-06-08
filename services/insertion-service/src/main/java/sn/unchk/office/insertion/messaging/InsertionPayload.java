package sn.unchk.office.insertion.messaging;

import sn.unchk.office.insertion.domain.InsertionOutcome;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Charge utile d'un événement « situation d'insertion » publié sur {@code insertion.events}.
 * <p>
 * C'est l'événement clé pour les statistiques côté admin-service (auto-emploi vs salarié).
 */
public record InsertionPayload(
        UUID id,
        UUID studentRef,
        UUID formationRef,
        String kind,
        LocalDate observedAt,
        boolean current
) {

    public static InsertionPayload depuis(InsertionOutcome o) {
        return new InsertionPayload(
                o.getId(),
                o.getStudentRef(),
                o.getFormationRef(),
                o.getKind() != null ? o.getKind().name() : null,
                o.getObservedAt(),
                o.isCurrent());
    }
}
