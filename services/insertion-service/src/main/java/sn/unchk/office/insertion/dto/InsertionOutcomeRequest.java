package sn.unchk.office.insertion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.insertion.domain.InsertionKind;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données de saisie d'une situation d'insertion (support des statistiques).
 *
 * @param studentRef   étudiant concerné → people.students.id (obligatoire)
 * @param formationRef formation → academic.formations.id (pour les stats par formation)
 * @param kind         situation d'insertion (obligatoire : auto-emploi, salarié...)
 * @param employerName employeur / auto-entreprise
 * @param jobTitle     intitulé du poste
 * @param observedAt   date de constat (aujourd'hui par défaut si absent)
 * @param current      situation courante (vrai par défaut)
 */
public record InsertionOutcomeRequest(
        @NotNull(message = "L'étudiant (studentRef) est obligatoire.")
        UUID studentRef,

        UUID formationRef,

        @NotNull(message = "La situation d'insertion (kind) est obligatoire.")
        InsertionKind kind,

        @Size(max = 255)
        String employerName,

        @Size(max = 255)
        String jobTitle,

        LocalDate observedAt,

        Boolean current
) {
}
