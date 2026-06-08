package sn.unchk.office.admin.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Charge utile consommée depuis le topic {@code people.staff} (canonique du personnel).
 * <p>
 * Seuls les champs utiles au read-model local {@code people_staff_ro} sont déclarés ;
 * les autres champs éventuels du message sont ignorés ({@link JsonIgnoreProperties}) pour
 * tolérer les évolutions additives du schéma (cf. docs/architecture.md, eventVersion).
 *
 * @param id         identifiant canonique du personnel (= people.staff.id)
 * @param fullName   nom complet affiché
 * @param kind       type de personnel (enseignant, administratif...)
 * @param department département de rattachement
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeopleStaffPayload(
        UUID id,
        String fullName,
        String kind,
        String department
) {
}
