package sn.unchk.office.admin.authz;

import org.springframework.stereotype.Component;
import sn.unchk.office.admin.domain.AdminCommunique;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.Mail;
import sn.unchk.office.admin.repository.AdminCommuniqueRepository;
import sn.unchk.office.admin.repository.BudgetRepository;
import sn.unchk.office.admin.repository.MailRepository;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.authz.FournisseurAttributsRessource;

import java.util.List;
import java.util.UUID;

/**
 * Fournisseur d'attributs ABAC du service Administration (anti-IDOR).
 * <p>
 * Appelé par le {@code ResourceAccessGuard} (libs/common) pour enrichir la ressource envoyée à
 * OPA avec son {@code ownerId} et sa {@code visibility} RÉELS, chargés depuis la base locale.
 * Ainsi, OPA décide en connaissance de cause : seul le propriétaire (ou un rôle autorisé, ou
 * l'admin) accède à l'objet désigné par son UUID — même si l'UUID est deviné.
 * <p>
 * Les états budgétaires sont des données administratives sensibles : leur visibilité par défaut
 * est restreinte aux rôles {@code administratif} et {@code admin}.
 */
@Component
public class FournisseurAttributsAdmin implements FournisseurAttributsRessource {

    /** Rôles autorisés à consulter les données administratives par défaut (visibilité ABAC). */
    private static final List<String> VISIBILITE_ADMIN = List.of("admin", "administratif");

    private final BudgetRepository budgetRepository;
    private final MailRepository mailRepository;
    private final AdminCommuniqueRepository communiqueRepository;

    public FournisseurAttributsAdmin(BudgetRepository budgetRepository,
                                     MailRepository mailRepository,
                                     AdminCommuniqueRepository communiqueRepository) {
        this.budgetRepository = budgetRepository;
        this.mailRepository = mailRepository;
        this.communiqueRepository = communiqueRepository;
    }

    @Override
    public EntreeOpa.Ressource attributs(String type, String id) {
        return switch (type) {
            case "budget" -> attributsBudget(id);
            case "courrier" -> attributsCourrier(id);
            case "communique" -> attributsCommunique(id);
            // Type inconnu : ressource minimale, OPA tranche avec ses propres données.
            default -> new EntreeOpa.Ressource(type, id, null, List.of());
        };
    }

    /** Charge le propriétaire et la visibilité d'un budget pour la décision OPA. */
    private EntreeOpa.Ressource attributsBudget(String id) {
        UUID budgetId = parserUuid(id);
        String ownerId = null;
        if (budgetId != null) {
            ownerId = budgetRepository.findById(budgetId)
                    .map(Budget::getOwnerId)
                    .map(UUID::toString)
                    .orElse(null);
        }
        return new EntreeOpa.Ressource("budget", id, ownerId, VISIBILITE_ADMIN);
    }

    /** Charge le propriétaire (auteur) et la visibilité d'un courrier pour la décision OPA. */
    private EntreeOpa.Ressource attributsCourrier(String id) {
        UUID mailId = parserUuid(id);
        String ownerId = null;
        if (mailId != null) {
            ownerId = mailRepository.findById(mailId)
                    .map(Mail::getCreatedBy)
                    .map(UUID::toString)
                    .orElse(null);
        }
        return new EntreeOpa.Ressource("courrier", id, ownerId, VISIBILITE_ADMIN);
    }

    /** Charge le propriétaire (auteur) et la visibilité d'un communiqué pour la décision OPA. */
    private EntreeOpa.Ressource attributsCommunique(String id) {
        UUID communiqueId = parserUuid(id);
        String ownerId = null;
        if (communiqueId != null) {
            ownerId = communiqueRepository.findById(communiqueId)
                    .map(AdminCommunique::getCreatedBy)
                    .map(UUID::toString)
                    .orElse(null);
        }
        return new EntreeOpa.Ressource("communique", id, ownerId, VISIBILITE_ADMIN);
    }

    /** Parse prudent : un UUID invalide laisse OPA refuser (deny-by-default). */
    private UUID parserUuid(String valeur) {
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
