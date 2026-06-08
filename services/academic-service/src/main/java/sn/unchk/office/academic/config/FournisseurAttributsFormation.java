package sn.unchk.office.academic.config;

import org.springframework.stereotype.Component;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.academic.formation.FormationRepository;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;

import java.util.List;
import java.util.UUID;

/**
 * Fournit à OPA les attributs ABAC des ressources du domaine académique (anti-IDOR).
 * <p>
 * Appelé par le {@link sn.unchk.office.common.authz.ResourceAccessGuard} lorsqu'un endpoint
 * sensible est annoté {@link sn.unchk.office.common.authz.VerifieAccesObjet}. Pour une
 * formation, on charge l'objet local et on en déduit :
 * <ul>
 *   <li>{@code ownerId} : le responsable de la formation (people.staff.id) ou, à défaut,
 *       son créateur — permet la règle « propriétaire » d'OPA ;</li>
 *   <li>{@code visibility} : les rôles autorisés à consulter une formation
 *       (lecture large conforme à la matrice rôles × modules).</li>
 * </ul>
 * Si la ressource n'existe pas, on renvoie une ressource « vide » : OPA refusera (deny-by-default)
 * et la garde renverra une erreur générique (anti-énumération).
 */
@Component
public class FournisseurAttributsFormation implements FournisseurAttributsRessource {

    /**
     * Rôles autorisés à consulter une formation (vue lecture, cf. matrice rôles × modules :
     * Formations = Gestion pour admin/administratif/enseignant, Consultation pour appui-insertion,
     * Propre périmètre pour étudiant). Le filtrage fin propriétaire reste géré par OPA.
     */
    private static final List<String> VISIBILITE_FORMATION =
            List.of("admin", "administratif", "enseignant", "appui-insertion", "etudiant");

    private final FormationRepository formationRepository;

    public FournisseurAttributsFormation(FormationRepository formationRepository) {
        this.formationRepository = formationRepository;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        if (!"formation".equals(type)) {
            // Type inconnu de ce service : ressource minimale, OPA tranche.
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        UUID formationId = parseUuid(id);
        if (formationId == null) {
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }

        return formationRepository.findByIdAndDeletedAtIsNull(formationId)
                .map(this::versRessource)
                // Inexistante : pas de propriétaire ni de visibilité -> OPA refuse (404 anti-énumération).
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    /** Construit la ressource OPA enrichie à partir de la formation chargée. */
    private EntreeOpa.Ressource versRessource(Formation formation) {
        UUID owner = formation.getResponsibleRef() != null
                ? formation.getResponsibleRef()
                : formation.getCreatedBy();
        return new EntreeOpa.Ressource(
                "formation",
                formation.getId().toString(),
                owner != null ? owner.toString() : null,
                VISIBILITE_FORMATION);
    }

    /** Convertit prudemment une chaîne en UUID ; renvoie {@code null} si le format est invalide. */
    private UUID parseUuid(String valeur) {
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
