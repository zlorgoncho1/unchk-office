package sn.unchk.office.people.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.StudentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Donnees de mise a jour d'un etudiant.
 * <p>
 * L'INE n'est pas modifiable (identifiant national stable) : il n'apparait pas ici.
 */
public record ModifierEtudiantRequest(

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

        LocalDate birthDate,

        @Size(max = 255, message = "Le lieu de naissance ne peut depasser 255 caracteres.")
        String birthPlace,

        @Email(message = "Le courriel n'est pas valide.")
        @Size(max = 255, message = "Le courriel ne peut depasser 255 caracteres.")
        String email,

        @Size(max = 32, message = "Le telephone ne peut depasser 32 caracteres.")
        String phone,

        @Size(max = 500, message = "L'adresse ne peut depasser 500 caracteres.")
        String address,

        @Size(max = 512, message = "La cle photo ne peut depasser 512 caracteres.")
        String photoObjectKey,

        UUID formationRef,

        @Size(max = 32, message = "La promotion ne peut depasser 32 caracteres.")
        String promotion,

        @Min(value = 1990, message = "L'annee de debut doit etre posterieure a 1990.")
        @Max(value = 2100, message = "L'annee de debut est invalide.")
        Short enrollmentYear,

        @Min(value = 1990, message = "L'annee de sortie doit etre posterieure a 1990.")
        @Max(value = 2100, message = "L'annee de sortie est invalide.")
        Short exitYear,

        UUID userRef,

        @NotNull(message = "Le statut est obligatoire.")
        StudentStatus status,

        @Valid
        List<DiplomeDto> diplomas
) {
}
