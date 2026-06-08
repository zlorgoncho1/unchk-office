package sn.unchk.office.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.BudgetLine;
import sn.unchk.office.admin.domain.BudgetStatus;
import sn.unchk.office.admin.dto.BudgetDto;
import sn.unchk.office.admin.dto.BudgetResumeDto;
import sn.unchk.office.admin.dto.ChangementStatutBudgetDto;
import sn.unchk.office.admin.dto.CreationBudgetDto;
import sn.unchk.office.admin.dto.CreationLigneBudgetaireDto;
import sn.unchk.office.admin.dto.MajBudgetDto;
import sn.unchk.office.admin.dto.RealisationLigneDto;
import sn.unchk.office.admin.mapper.BudgetMapper;
import sn.unchk.office.admin.messaging.BudgetEventProducer;
import sn.unchk.office.admin.repository.BudgetLineRepository;
import sn.unchk.office.admin.repository.BudgetRepository;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service métier de gestion budgétaire (projet de budget, note d'orientation, budget réalisé).
 * <p>
 * Règles clés :
 * <ul>
 *   <li>Unicité (exercice, libellé) à la création (sinon 409).</li>
 *   <li>Les totaux prévu/réalisé sont recalculés à partir des lignes après chaque modification.</li>
 *   <li>Toute évolution d'état publie un événement sur {@code admin.budget} (CQRS, zéro REST).</li>
 *   <li>Le propriétaire ({@code ownerId}) et l'auteur ({@code createdBy}) proviennent du JWT,
 *       jamais du corps client (anti sur-affectation / anti-IDOR).</li>
 * </ul>
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final BudgetMapper mapper;
    private final BudgetEventProducer producteur;
    private final AuditLogger audit;

    public BudgetService(BudgetRepository budgetRepository,
                         BudgetLineRepository budgetLineRepository,
                         BudgetMapper mapper,
                         BudgetEventProducer producteur,
                         AuditLogger audit) {
        this.budgetRepository = budgetRepository;
        this.budgetLineRepository = budgetLineRepository;
        this.mapper = mapper;
        this.producteur = producteur;
        this.audit = audit;
    }

    /** Crée un projet de budget pour un exercice. */
    @Transactional
    public BudgetDto creer(CreationBudgetDto dto) {
        if (budgetRepository.existsByFiscalYearAndLabel(dto.fiscalYear(), dto.label())) {
            throw new ConflitRessourceException("Un budget existe déjà pour cet exercice et ce libellé.");
        }
        UUID sujet = sujetCourant();
        Budget budget = new BudgetBuilder(dto, sujet).construire();
        Budget enregistre = budgetRepository.save(budget);

        audit.succes("CREATION_BUDGET", "budget", enregistre.getId().toString());
        producteur.publier("BudgetCree", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /** Met à jour les attributs modifiables d'un budget (libellé, note d'orientation, devise). */
    @Transactional
    public BudgetDto mettreAJour(UUID budgetId, MajBudgetDto dto) {
        Budget budget = chargerOuLever(budgetId);
        budget.setLabel(dto.label());
        budget.setOrientationNote(dto.orientationNote());
        if (dto.currency() != null) {
            budget.setCurrency(dto.currency());
        }
        Budget enregistre = budgetRepository.save(budget);

        audit.succes("MAJ_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetMisAJour", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /**
     * Supprime un budget et toutes ses lignes.
     * <p>
     * Le modèle Budget ne porte pas de {@code deletedAt} : on effectue donc une suppression
     * physique (budget + lignes), puis on publie un événement {@code BudgetSupprime} sur
     * {@code admin.budget} pour que les read-models des autres services se purgent à leur tour.
     */
    @Transactional
    public void supprimer(UUID budgetId) {
        Budget budget = chargerOuLever(budgetId);
        // On capture l'état avant suppression : il sert de charge utile à l'événement
        // (type inféré via var, comme les autres méthodes, pour éviter un import).
        var payload = mapper.versPayload(budget);

        // Suppression physique : d'abord les lignes (dépendantes), puis l'entête.
        budgetLineRepository.deleteByBudgetId(budgetId);
        budgetRepository.delete(budget);

        audit.succes("SUPPRESSION_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetSupprime", payload);
    }

    /** Fait évoluer le statut d'un budget (projet → voté → en exécution → clôturé). */
    @Transactional
    public BudgetDto changerStatut(UUID budgetId, ChangementStatutBudgetDto dto) {
        Budget budget = chargerOuLever(budgetId);
        budget.setStatus(dto.status());
        Budget enregistre = budgetRepository.save(budget);

        audit.succes("CHANGEMENT_STATUT_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetStatutModifie", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /** Ajoute une ligne budgétaire (poste prévu) puis recalcule les totaux. */
    @Transactional
    public BudgetDto ajouterLigne(UUID budgetId, CreationLigneBudgetaireDto dto) {
        Budget budget = chargerOuLever(budgetId);

        BudgetLine ligne = new BudgetLine();
        ligne.setBudgetId(budget.getId());
        ligne.setCategory(dto.category());
        ligne.setDirection(dto.direction());
        ligne.setPlannedAmount(dto.plannedAmount());
        ligne.setRealizedAmount(BigDecimal.ZERO);
        ligne.setLabel(dto.label());
        budgetLineRepository.save(ligne);

        Budget enregistre = recalculerTotaux(budget);
        audit.succes("AJOUT_LIGNE_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetMisAJour", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /** Saisit le montant réalisé d'une ligne (budget réalisé) puis recalcule les totaux. */
    @Transactional
    public BudgetDto renseignerRealisation(UUID budgetId, UUID ligneId, RealisationLigneDto dto) {
        Budget budget = chargerOuLever(budgetId);
        BudgetLine ligne = budgetLineRepository.findById(ligneId)
                .filter(l -> l.getBudgetId().equals(budgetId))
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne budgétaire introuvable."));

        ligne.setRealizedAmount(dto.realizedAmount());
        budgetLineRepository.save(ligne);

        Budget enregistre = recalculerTotaux(budget);
        audit.succes("REALISATION_LIGNE_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetRealiseMisAJour", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /** Supprime une ligne budgétaire puis recalcule les totaux du budget. */
    @Transactional
    public BudgetDto supprimerLigne(UUID budgetId, UUID ligneId) {
        Budget budget = chargerOuLever(budgetId);
        BudgetLine ligne = budgetLineRepository.findById(ligneId)
                .filter(l -> l.getBudgetId().equals(budgetId))
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne budgétaire introuvable."));

        budgetLineRepository.delete(ligne);

        Budget enregistre = recalculerTotaux(budget);
        audit.succes("SUPPRESSION_LIGNE_BUDGET", "budget", budgetId.toString());
        producteur.publier("BudgetMisAJour", mapper.versPayload(enregistre));
        return chargerDto(enregistre);
    }

    /** Consulte un budget (entête + lignes). */
    @Transactional(readOnly = true)
    public BudgetDto consulter(UUID budgetId) {
        return chargerDto(chargerOuLever(budgetId));
    }

    /** Liste les budgets, éventuellement filtrés par exercice. */
    @Transactional(readOnly = true)
    public List<BudgetResumeDto> lister(Short fiscalYear) {
        List<Budget> budgets = (fiscalYear != null)
                ? budgetRepository.findByFiscalYearOrderByLabelAsc(fiscalYear)
                : budgetRepository.findAll();
        return budgets.stream().map(mapper::versResumeDto).toList();
    }

    /** Renvoie les lignes d'un budget (utilisé par les exports). */
    @Transactional(readOnly = true)
    public List<BudgetLine> lignesDuBudget(UUID budgetId) {
        chargerOuLever(budgetId);
        return budgetLineRepository.findByBudgetIdOrderByCategoryAsc(budgetId);
    }

    // ----------------------------------------------------------------
    // Internes
    // ----------------------------------------------------------------

    /** Recalcule total prévu / réalisé d'un budget à partir de ses lignes et persiste. */
    private Budget recalculerTotaux(Budget budget) {
        List<BudgetLine> lignes = budgetLineRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());
        BigDecimal prevu = lignes.stream()
                .map(BudgetLine::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realise = lignes.stream()
                .map(BudgetLine::getRealizedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        budget.setTotalPlanned(prevu);
        budget.setTotalRealized(realise);
        return budgetRepository.save(budget);
    }

    /** Charge le DTO détaillé (entête + lignes) d'un budget. */
    private BudgetDto chargerDto(Budget budget) {
        List<BudgetLine> lignes = budgetLineRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());
        return mapper.versDto(budget, lignes);
    }

    /** Charge un budget ou lève une 404 si introuvable (anti-énumération). */
    private Budget chargerOuLever(UUID budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RessourceIntrouvableException("Budget introuvable."));
    }

    /** Récupère l'UUID du sujet courant (claim sub) ou {@code null} hors contexte authentifié. */
    private UUID sujetCourant() {
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        if (sujet.id() == null) {
            return null;
        }
        try {
            return UUID.fromString(sujet.id());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Petit constructeur interne pour assembler l'entité Budget à partir du DTO + sujet. */
    private static final class BudgetBuilder {
        private final CreationBudgetDto dto;
        private final UUID sujet;

        BudgetBuilder(CreationBudgetDto dto, UUID sujet) {
            this.dto = dto;
            this.sujet = sujet;
        }

        Budget construire() {
            Budget budget = new Budget();
            budget.setFiscalYear(dto.fiscalYear());
            budget.setLabel(dto.label());
            budget.setOrientationNote(dto.orientationNote());
            budget.setStatus(BudgetStatus.projet);
            budget.setCurrency(dto.currency() != null ? dto.currency() : "XOF");
            budget.setTotalPlanned(BigDecimal.ZERO);
            budget.setTotalRealized(BigDecimal.ZERO);
            // Propriétaire et auteur issus du JWT : jamais du corps client (anti-IDOR).
            budget.setOwnerId(sujet);
            budget.setCreatedBy(sujet);
            return budget;
        }
    }
}
