package sn.unchk.office.people.messaging.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import sn.unchk.office.people.domain.Student;

import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile (event-carried state transfer) du topic {@code people.students}.
 * <p>
 * Porte l'etat canonique de l'etudiant : les consommateurs (academic, insertion,
 * communication, document, admin) construisent leurs read-models {@code _ro} a partir
 * de ce payload, sans aucun appel REST. Evolutions de schema additives uniquement.
 *
 * @param studentId      UUID canonique de l'etudiant (= cle de partition)
 * @param ine            identifiant national etudiant
 * @param matricule      matricule interne UNCHK
 * @param firstName      prenom
 * @param lastName       nom
 * @param gender         genre (stats par genre cote insertion)
 * @param email          courriel
 * @param formationRef   formation rattachee (UUID)
 * @param promotion      promotion
 * @param enrollmentYear annee de debut
 * @param exitYear       annee de sortie
 * @param otherTrainings autres formations (texte libre)
 * @param status         statut (inscrit, diplome, abandon, suspendu)
 * @param updatedAt      horodatage de la derniere modification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentPayload(
        UUID studentId,
        String ine,
        String matricule,
        String firstName,
        String lastName,
        String gender,
        String email,
        UUID formationRef,
        String promotion,
        Short enrollmentYear,
        Short exitYear,
        String otherTrainings,
        String status,
        Instant updatedAt
) {

    /** Projette l'etat courant d'une entite etudiant vers le payload Kafka. */
    public static StudentPayload depuis(Student s) {
        return new StudentPayload(
                s.getId(),
                s.getIne(),
                s.getMatricule(),
                s.getFirstName(),
                s.getLastName(),
                s.getGender() != null ? s.getGender().name() : null,
                s.getEmail(),
                s.getFormationRef(),
                s.getPromotion(),
                s.getEnrollmentYear(),
                s.getExitYear(),
                s.getOtherTrainings(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getUpdatedAt());
    }
}
