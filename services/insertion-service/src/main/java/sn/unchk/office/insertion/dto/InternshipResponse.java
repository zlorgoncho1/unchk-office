package sn.unchk.office.insertion.dto;

import sn.unchk.office.insertion.domain.Internship;
import sn.unchk.office.insertion.domain.InternshipStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation d'un stage renvoyée au client.
 */
public record InternshipResponse(
        UUID id,
        UUID studentRef,
        UUID partnerId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        InternshipStatus status,
        UUID tutorRef,
        String supervisorName,
        UUID reportRef,
        BigDecimal grade
) {

    public static InternshipResponse depuis(Internship s) {
        return new InternshipResponse(
                s.getId(),
                s.getStudentRef(),
                s.getPartnerId(),
                s.getTitle(),
                s.getStartDate(),
                s.getEndDate(),
                s.getStatus(),
                s.getTutorRef(),
                s.getSupervisorName(),
                s.getReportRef(),
                s.getGrade());
    }
}
