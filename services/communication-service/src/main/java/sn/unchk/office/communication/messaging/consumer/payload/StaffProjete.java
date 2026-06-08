package sn.unchk.office.communication.messaging.consumer.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Vue partielle de l'état d'un personnel reçu sur {@code people.staff}.
 * <p>
 * Alimente la projection {@code people_staff_ro} (nom de l'auteur / organisateur). Champs
 * inconnus ignorés (évolutions additives).
 *
 * @param id       identifiant du personnel (people.staff.id)
 * @param fullName nom complet
 * @param kind     type de personnel (enseignant, tuteur...)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffProjete(
        UUID id,
        String fullName,
        String kind
) {
}
