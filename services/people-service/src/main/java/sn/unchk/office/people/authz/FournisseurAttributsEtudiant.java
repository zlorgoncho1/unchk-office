package sn.unchk.office.people.authz;

import org.springframework.stereotype.Component;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;
import sn.unchk.office.people.domain.Student;
import sn.unchk.office.people.repository.StudentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Fournisseur d'attributs ABAC (anti-IDOR) pour les ressources du people-service.
 * <p>
 * Implemente le point d'extension {@link FournisseurAttributsRessource} de la librairie
 * commune : le {@code ResourceAccessGuard} l'appelle pour enrichir la ressource envoyee
 * a OPA avec son {@code ownerId} (compte etudiant proprietaire) et sa {@code visibility}
 * (roles autorises a consulter une fiche etudiant). OPA tranche alors :
 * <ul>
 *   <li>le proprietaire (etudiant via {@code userRef}) accede a SA fiche ;</li>
 *   <li>les roles de gestion ({@code admin}, {@code administratif}, {@code enseignant},
 *       {@code appui-insertion}) accedent selon la visibilite.</li>
 * </ul>
 * Pour un identifiant inconnu, on renvoie une ressource minimale : OPA refusera
 * (deny-by-default), et le service traduira en 404 (anti-enumeration).
 */
@Component
public class FournisseurAttributsEtudiant implements FournisseurAttributsRessource {

    /** Type logique reconnu par ce fournisseur. */
    public static final String TYPE_ETUDIANT = "etudiant";

    /** Roles de gestion autorises a consulter une fiche etudiant (visibilite par defaut). */
    private static final List<String> VISIBILITE_FICHE_ETUDIANT =
            List.of("admin", "administratif", "enseignant", "appui-insertion");

    private final StudentRepository studentRepository;

    public FournisseurAttributsEtudiant(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        if (!TYPE_ETUDIANT.equals(type)) {
            // Type non gere ici : ressource minimale, decision laissee a OPA.
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        UUID studentId = parseUuid(id);
        if (studentId == null) {
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        return studentRepository.findActifById(studentId)
                .map(this::versRessource)
                // Inconnu : aucune visibilite -> OPA refuse -> 404 cote service.
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    private EntreeOpa.Ressource versRessource(Student etudiant) {
        // Le proprietaire est le compte identity lie a l'etudiant (acces a sa propre fiche).
        String ownerId = etudiant.getUserRef() != null ? etudiant.getUserRef().toString() : null;
        return new EntreeOpa.Ressource(
                TYPE_ETUDIANT,
                etudiant.getId().toString(),
                ownerId,
                VISIBILITE_FICHE_ETUDIANT);
    }

    private UUID parseUuid(String valeur) {
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }
}
