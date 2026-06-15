package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.admin.domain.MailDirection;
import sn.unchk.office.admin.domain.MailStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données d'entrée pour enregistrer un courrier (arrivé / départ).
 * <p>
 * DTO dédié (jamais l'entité JPA en {@code @RequestBody}) : {@code id}, {@code createdBy},
 * horodatages et {@code version} ne sont jamais liés depuis le client (anti sur-affectation).
 *
 * @param direction     sens du courrier (arrivé / départ)
 * @param subject       objet
 * @param correspondent correspondant (expéditeur ou destinataire)
 * @param mailDate      date du courrier
 * @param status        statut initial (optionnel ; « recu » par défaut)
 * @param assignedTo    agent en charge (→ people.staff.id, optionnel)
 * @param reference     référence / numéro de courrier (optionnel, unique si renseigné)
 * @param notes         annotations libres (optionnel)
 */
public record CreationMailDto(
        @NotNull(message = "Le sens du courrier est obligatoire.")
        MailDirection direction,

        @NotBlank(message = "L'objet est obligatoire.")
        @Size(max = 2000, message = "L'objet est trop long.")
        String subject,

        @NotBlank(message = "Le correspondant est obligatoire.")
        @Size(max = 500, message = "Le correspondant est trop long.")
        String correspondent,

        @NotNull(message = "La date du courrier est obligatoire.")
        LocalDate mailDate,

        MailStatus status,

        UUID assignedTo,

        @Size(max = 64, message = "La référence est trop longue.")
        String reference,

        @Size(max = 4000, message = "Les annotations sont trop longues.")
        String notes
) {
}
