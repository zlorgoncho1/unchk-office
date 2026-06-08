package sn.unchk.office.insertion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.insertion.domain.InternshipStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Données de création / mise à jour d'un stage (bilan de stage).
 *
 * @param studentRef     étudiant concerné → people.students.id (obligatoire)
 * @param partnerId      partenaire d'accueil (FK locale, facultatif)
 * @param title          intitulé du stage (obligatoire)
 * @param startDate      date de début
 * @param endDate        date de fin (doit être ≥ début, vérifié en base)
 * @param status         statut du stage (prevu par défaut)
 * @param tutorRef       tuteur académique → people.staff.id
 * @param supervisorName maître de stage côté partenaire
 * @param reportRef      rapport de stage → document.documents.id
 * @param grade          note (0 à 20)
 */
public record InternshipRequest(
        @NotNull(message = "L'étudiant (studentRef) est obligatoire.")
        UUID studentRef,

        UUID partnerId,

        @NotBlank(message = "L'intitulé du stage est obligatoire.")
        @Size(max = 255)
        String title,

        LocalDate startDate,

        LocalDate endDate,

        InternshipStatus status,

        UUID tutorRef,

        @Size(max = 255)
        String supervisorName,

        UUID reportRef,

        @DecimalMin(value = "0.0", message = "La note minimale est 0.")
        @DecimalMax(value = "20.0", message = "La note maximale est 20.")
        BigDecimal grade
) {
}
