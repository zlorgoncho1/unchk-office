package sn.unchk.office.academic.formateur.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile attendue sur le topic {@code people.staff} (personnel / formateur canonique).
 * <p>
 * Le academic-service consomme ce payload pour alimenter sa projection locale
 * {@code academic_formateur_ro} (afficher les noms des formateurs sans appel REST).
 * Les champs non utilisés ici sont ignorés à la désérialisation pour rester tolérant
 * aux évolutions additives du producteur.
 *
 * @param staffId    identifiant canonique (= people.staff.id)
 * @param firstName  prénom
 * @param lastName   nom
 * @param kind       type de personnel (enseignant, tuteur...)
 * @param speciality spécialité
 * @param active     en activité
 * @param deletedAt  instant de suppression logique (tombstone), {@code null} si actif
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffPayload(
        UUID staffId,
        String firstName,
        String lastName,
        String kind,
        String speciality,
        Boolean active,
        Instant deletedAt
) {

    /** Compose un nom complet lisible à partir du prénom et du nom. */
    public String nomComplet() {
        String prenom = firstName != null ? firstName.trim() : "";
        String nom = lastName != null ? lastName.trim() : "";
        return (prenom + " " + nom).trim();
    }
}
