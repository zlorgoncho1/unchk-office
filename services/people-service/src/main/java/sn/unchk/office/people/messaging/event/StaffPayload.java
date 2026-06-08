package sn.unchk.office.people.messaging.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import sn.unchk.office.people.domain.Staff;

import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile (event-carried state transfer) du topic {@code people.staff}.
 * <p>
 * Porte l'etat canonique du personnel / formateur. Permet notamment a academic-service
 * de construire son read-model {@code academic_formateur_ro} (afficher les formateurs
 * sans REST), et a communication/admin d'afficher l'auteur / l'agent en charge.
 *
 * @param staffId    UUID canonique du personnel (= cle de partition)
 * @param matricule  matricule
 * @param firstName  prenom
 * @param lastName   nom
 * @param gender     genre
 * @param kind       type de personnel (enseignant, tuteur...)
 * @param email      courriel
 * @param grade      grade / fonction
 * @param speciality specialite (formateur)
 * @param department departement
 * @param active     en activite
 * @param updatedAt  horodatage de la derniere modification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffPayload(
        UUID staffId,
        String matricule,
        String firstName,
        String lastName,
        String gender,
        String kind,
        String email,
        String grade,
        String speciality,
        String department,
        boolean active,
        Instant updatedAt
) {

    /** Projette l'etat courant d'une entite personnel vers le payload Kafka. */
    public static StaffPayload depuis(Staff s) {
        return new StaffPayload(
                s.getId(),
                s.getMatricule(),
                s.getFirstName(),
                s.getLastName(),
                s.getGender() != null ? s.getGender().name() : null,
                s.getKind() != null ? s.getKind().name() : null,
                s.getEmail(),
                s.getGrade(),
                s.getSpeciality(),
                s.getDepartment(),
                s.isActive(),
                s.getUpdatedAt());
    }
}
