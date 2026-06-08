package sn.unchk.office.insertion.dto;

import sn.unchk.office.insertion.domain.InsertionKind;
import sn.unchk.office.insertion.domain.InsertionOutcome;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation d'une situation d'insertion renvoyée au client.
 */
public record InsertionOutcomeResponse(
        UUID id,
        UUID studentRef,
        UUID formationRef,
        InsertionKind kind,
        String employerName,
        String jobTitle,
        LocalDate observedAt,
        boolean current
) {

    public static InsertionOutcomeResponse depuis(InsertionOutcome o) {
        return new InsertionOutcomeResponse(
                o.getId(),
                o.getStudentRef(),
                o.getFormationRef(),
                o.getKind(),
                o.getEmployerName(),
                o.getJobTitle(),
                o.getObservedAt(),
                o.isCurrent());
    }
}
