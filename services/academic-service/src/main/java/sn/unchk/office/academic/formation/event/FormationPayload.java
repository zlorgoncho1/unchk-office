package sn.unchk.office.academic.formation.event;

import sn.unchk.office.academic.formation.Formation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Charge utile publiée sur le topic {@code academic.formations} (transfert d'état).
 * <p>
 * Elle ne porte que l'état métier de la formation, en chaînes neutres pour les énumérations
 * (compatibilité ascendante : un consommateur d'un autre service n'a pas nos types Java).
 * Aucun secret ni donnée sensible n'y figure.
 *
 * @param formationId    identifiant de la formation (= clé de partition)
 * @param code           code unique
 * @param label          intitulé
 * @param level          niveau (chaîne)
 * @param kind           type (chaîne)
 * @param funding        financement (chaîne, peut être {@code null})
 * @param startDate      date de début
 * @param endDate        date de fin
 * @param trainedMale    formés (hommes)
 * @param trainedFemale  formés (femmes)
 * @param responsibleRef responsable (people.staff.id)
 * @param active         active
 */
public record FormationPayload(
        UUID formationId,
        String code,
        String label,
        String level,
        String kind,
        String funding,
        LocalDate startDate,
        LocalDate endDate,
        int trainedMale,
        int trainedFemale,
        UUID responsibleRef,
        boolean active
) {

    /** Construit le payload de transfert d'état à partir de l'entité. */
    public static FormationPayload de(Formation f) {
        return new FormationPayload(
                f.getId(),
                f.getCode(),
                f.getLabel(),
                f.getLevel() != null ? f.getLevel().valeurDb() : null,
                f.getKind() != null ? f.getKind().valeurDb() : null,
                f.getFunding() != null ? f.getFunding().valeurDb() : null,
                f.getStartDate(),
                f.getEndDate(),
                f.getTrainedMale(),
                f.getTrainedFemale(),
                f.getResponsibleRef(),
                f.isActive());
    }
}
