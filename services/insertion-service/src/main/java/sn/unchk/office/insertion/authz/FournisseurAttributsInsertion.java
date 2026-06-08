package sn.unchk.office.insertion.authz;

import org.springframework.stereotype.Component;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;
import sn.unchk.office.insertion.domain.Internship;
import sn.unchk.office.insertion.domain.Partner;
import sn.unchk.office.insertion.repository.InsertionOutcomeRepository;
import sn.unchk.office.insertion.repository.InternshipRepository;
import sn.unchk.office.insertion.repository.PartnerRepository;

import java.util.List;
import java.util.UUID;

/**
 * Fournisseur d'attributs ABAC du service d'insertion (anti-IDOR au niveau objet).
 * <p>
 * Appelé par la garde {@link sn.unchk.office.common.authz.ResourceAccessGuard} quand une
 * méthode est annotée {@link sn.unchk.office.common.authz.VerifieAccesObjet}. Il charge la
 * ressource depuis la base locale pour renseigner :
 * <ul>
 *   <li>{@code ownerId} = auteur de la fiche (createdBy) ; le propriétaire garde l'accès ;</li>
 *   <li>{@code visibility} = rôles autorisés en lecture sur les fiches d'insertion.</li>
 * </ul>
 * OPA tranche ensuite : un étudiant ne peut consulter QUE ses propres objets (cf. règle
 * {@code object_visible}), même s'il devine l'UUID d'un autre objet.
 */
@Component
public class FournisseurAttributsInsertion implements FournisseurAttributsRessource {

    /** Rôles autorisés en lecture sur les fiches d'insertion (gestion du module). */
    private static final List<String> VISIBILITE_GESTION =
            List.of("admin", "administratif", "appui-insertion");

    private final InternshipRepository stages;
    private final PartnerRepository partenaires;
    private final InsertionOutcomeRepository situations;

    public FournisseurAttributsInsertion(InternshipRepository stages,
                                         PartnerRepository partenaires,
                                         InsertionOutcomeRepository situations) {
        this.stages = stages;
        this.partenaires = partenaires;
        this.situations = situations;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        UUID uuid = parseUuid(id);
        if (uuid == null) {
            // Identifiant illisible : ressource minimale, OPA refusera (deny-by-default hors admin).
            return new EntreeOpa.Ressource(type, id, null, List.of());
        }
        return switch (type) {
            case "stage", "internship" -> ressourceStage(type, id, uuid);
            case "partenaire", "partner" -> ressourcePartenaire(type, id, uuid);
            case "insertion", "outcome" -> ressourceSituation(type, id, uuid);
            // Type inconnu : ressource minimale.
            default -> new EntreeOpa.Ressource(type, id, null, List.of());
        };
    }

    /**
     * Pour un stage : le propriétaire ABAC est l'étudiant concerné (studentRef), afin que
     * l'étudiant puisse consulter SON bilan de stage ; l'appui-insertion voit tout via la visibilité.
     */
    private EntreeOpa.Ressource ressourceStage(String type, String id, UUID uuid) {
        return stages.findByIdAndDeletedAtIsNull(uuid)
                .map((Internship s) -> new EntreeOpa.Ressource(
                        type, id,
                        s.getStudentRef() != null ? s.getStudentRef().toString() : null,
                        VISIBILITE_GESTION))
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    /** Un partenaire est géré par le module ; pas de propriétaire « étudiant ». */
    private EntreeOpa.Ressource ressourcePartenaire(String type, String id, UUID uuid) {
        return partenaires.findByIdAndDeletedAtIsNull(uuid)
                .map((Partner p) -> new EntreeOpa.Ressource(
                        type, id,
                        p.getCreatedBy() != null ? p.getCreatedBy().toString() : null,
                        VISIBILITE_GESTION))
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    /** Pour une situation d'insertion : propriétaire = étudiant concerné (consultation de son suivi). */
    private EntreeOpa.Ressource ressourceSituation(String type, String id, UUID uuid) {
        return situations.findById(uuid)
                .map(o -> new EntreeOpa.Ressource(
                        type, id,
                        o.getStudentRef() != null ? o.getStudentRef().toString() : null,
                        VISIBILITE_GESTION))
                .orElseGet(() -> new EntreeOpa.Ressource(type, id, null, List.of()));
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }
}
