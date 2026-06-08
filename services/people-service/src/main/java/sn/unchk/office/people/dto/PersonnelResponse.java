package sn.unchk.office.people.dto;

import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.Staff;
import sn.unchk.office.people.domain.StaffKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue de lecture d'un membre du personnel / formateur renvoyee par l'API.
 */
public record PersonnelResponse(
        UUID id,
        String matricule,
        String firstName,
        String lastName,
        Genre gender,
        StaffKind kind,
        String email,
        String phone,
        String grade,
        String speciality,
        String department,
        String photoObjectKey,
        boolean active,
        LocalDate hiredAt,
        Instant createdAt,
        Instant updatedAt
) {

    /** Convertit une entite personnel en vue de lecture. */
    public static PersonnelResponse depuis(Staff s) {
        return new PersonnelResponse(
                s.getId(),
                s.getMatricule(),
                s.getFirstName(),
                s.getLastName(),
                s.getGender(),
                s.getKind(),
                s.getEmail(),
                s.getPhone(),
                s.getGrade(),
                s.getSpeciality(),
                s.getDepartment(),
                s.getPhotoObjectKey(),
                s.isActive(),
                s.getHiredAt(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
