package sn.unchk.office.people.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Diplome d'un etudiant (entree et sortie).
 *
 * @param id         identifiant (null a la creation, renseigne en sortie)
 * @param label      intitule du diplome (obligatoire)
 * @param level      niveau (licence, master...)
 * @param obtainedAt date d'obtention
 */
public record DiplomeDto(
        UUID id,

        @NotBlank(message = "L'intitule du diplome est obligatoire.")
        @Size(max = 255, message = "L'intitule ne peut depasser 255 caracteres.")
        String label,

        @Size(max = 64, message = "Le niveau ne peut depasser 64 caracteres.")
        String level,

        LocalDate obtainedAt
) {
}
