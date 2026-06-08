package sn.unchk.office.insertion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données d'enregistrement d'un contact de suivi (registre de contact).
 *
 * @param studentRef  étudiant contacté → people.students.id (obligatoire)
 * @param contactedAt date du contact (aujourd'hui par défaut si absent)
 * @param channel     canal (téléphone, email, présentiel)
 * @param notes       compte rendu de l'échange
 * @param agentRef    agent d'appui à l'insertion → people.staff.id
 */
public record ContactLogRequest(
        @NotNull(message = "L'étudiant (studentRef) est obligatoire.")
        UUID studentRef,

        LocalDate contactedAt,

        @Size(max = 64)
        String channel,

        @Size(max = 4000)
        String notes,

        UUID agentRef
) {
}
