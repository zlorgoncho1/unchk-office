package sn.unchk.office.people.dto;

import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.Student;
import sn.unchk.office.people.domain.StudentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vue de lecture d'un etudiant renvoyee par l'API.
 * <p>
 * On n'expose jamais l'entite JPA directement : ce DTO controle precisement
 * les champs visibles (le {@code userRef} interne n'est pas divulgue).
 */
public record EtudiantResponse(
        UUID id,
        String ine,
        String matricule,
        String firstName,
        String lastName,
        Genre gender,
        LocalDate birthDate,
        String birthPlace,
        String email,
        String phone,
        String address,
        String photoObjectKey,
        UUID formationRef,
        String promotion,
        Short enrollmentYear,
        Short exitYear,
        StudentStatus status,
        List<DiplomeDto> diplomas,
        Instant createdAt,
        Instant updatedAt
) {

    /** Convertit une entite etudiant en vue de lecture. */
    public static EtudiantResponse depuis(Student s) {
        List<DiplomeDto> diplomes = s.getDiplomas().stream()
                .map(d -> new DiplomeDto(d.getId(), d.getLabel(), d.getLevel(), d.getObtainedAt()))
                .toList();
        return new EtudiantResponse(
                s.getId(),
                s.getIne(),
                s.getMatricule(),
                s.getFirstName(),
                s.getLastName(),
                s.getGender(),
                s.getBirthDate(),
                s.getBirthPlace(),
                s.getEmail(),
                s.getPhone(),
                s.getAddress(),
                s.getPhotoObjectKey(),
                s.getFormationRef(),
                s.getPromotion(),
                s.getEnrollmentYear(),
                s.getExitYear(),
                s.getStatus(),
                diplomes,
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
