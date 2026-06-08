package sn.unchk.office.people.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.StaffKind;

import java.time.LocalDate;

/**
 * Donnees de mise a jour d'un membre du personnel / formateur.
 */
public record ModifierPersonnelRequest(

        @Size(max = 32, message = "Le matricule ne peut depasser 32 caracteres.")
        String matricule,

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 255, message = "Le prenom ne peut depasser 255 caracteres.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 255, message = "Le nom ne peut depasser 255 caracteres.")
        String lastName,

        @NotNull(message = "Le genre est obligatoire.")
        Genre gender,

        @NotNull(message = "Le type de personnel est obligatoire.")
        StaffKind kind,

        @Email(message = "Le courriel n'est pas valide.")
        @Size(max = 255, message = "Le courriel ne peut depasser 255 caracteres.")
        String email,

        @Size(max = 32, message = "Le telephone ne peut depasser 32 caracteres.")
        String phone,

        @Size(max = 128, message = "Le grade ne peut depasser 128 caracteres.")
        String grade,

        @Size(max = 255, message = "La specialite ne peut depasser 255 caracteres.")
        String speciality,

        @Size(max = 128, message = "Le departement ne peut depasser 128 caracteres.")
        String department,

        @Size(max = 512, message = "La cle photo ne peut depasser 512 caracteres.")
        String photoObjectKey,

        @NotNull(message = "L'etat d'activite est obligatoire.")
        Boolean active,

        LocalDate hiredAt
) {
}
