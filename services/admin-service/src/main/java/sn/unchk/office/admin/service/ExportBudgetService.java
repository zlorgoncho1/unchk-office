package sn.unchk.office.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.BudgetLine;
import sn.unchk.office.admin.repository.BudgetLineRepository;
import sn.unchk.office.admin.repository.BudgetRepository;
import sn.unchk.office.common.export.ExcelExporter;
import sn.unchk.office.common.export.PdfExporter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Production des états budgétaires exportables (PDF / Excel) via les utilitaires de libs/common.
 * <p>
 * Met en forme l'état « prévu vs réalisé » d'un budget : une ligne par poste, avec l'écart
 * (prévu − réalisé), puis une ligne de total. Les exports sont des données déjà autorisées
 * (l'accès au budget est vérifié en amont par l'ABAC OPA au niveau objet).
 */
@Service
public class ExportBudgetService {

    /** Colonnes des états budgétaires exportés. */
    private static final List<String> ENTETES =
            List.of("Poste", "Sens", "Prévu", "Réalisé", "Écart");

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;

    public ExportBudgetService(BudgetRepository budgetRepository,
                               BudgetLineRepository budgetLineRepository,
                               PdfExporter pdfExporter,
                               ExcelExporter excelExporter) {
        this.budgetRepository = budgetRepository;
        this.budgetLineRepository = budgetLineRepository;
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
    }

    /** Génère l'état budgétaire au format PDF. */
    @Transactional(readOnly = true)
    public byte[] exporterPdf(UUID budgetId) {
        Budget budget = chargerOuLever(budgetId);
        return pdfExporter.exporter(titre(budget), ENTETES, lignesEtat(budget));
    }

    /** Génère l'état budgétaire au format Excel (xlsx). */
    @Transactional(readOnly = true)
    public byte[] exporterExcel(UUID budgetId) {
        Budget budget = chargerOuLever(budgetId);
        return excelExporter.exporter("Budget", ENTETES, lignesEtat(budget));
    }

    /** Titre lisible du rapport. */
    private String titre(Budget budget) {
        return "État budgétaire " + budget.getFiscalYear() + " — " + budget.getLabel();
    }

    /** Construit les lignes de l'état (postes + total) au format attendu par les exporteurs. */
    private List<List<String>> lignesEtat(Budget budget) {
        List<BudgetLine> lignes = budgetLineRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());
        List<List<String>> sortie = new ArrayList<>();
        for (BudgetLine ligne : lignes) {
            BigDecimal ecart = ligne.getPlannedAmount().subtract(ligne.getRealizedAmount());
            sortie.add(List.of(
                    valeur(ligne.getCategory()),
                    ligne.getDirection().name(),
                    ligne.getPlannedAmount().toPlainString(),
                    ligne.getRealizedAmount().toPlainString(),
                    ecart.toPlainString()));
        }
        // Ligne de total (prévu / réalisé / écart global).
        BigDecimal ecartGlobal = budget.getTotalPlanned().subtract(budget.getTotalRealized());
        sortie.add(List.of(
                "TOTAL",
                "-",
                budget.getTotalPlanned().toPlainString(),
                budget.getTotalRealized().toPlainString(),
                ecartGlobal.toPlainString()));
        return sortie;
    }

    /** Évite les cellules nulles dans les exports. */
    private String valeur(String v) {
        return v != null ? v : "";
    }

    /** Charge un budget ou lève une 404 (anti-énumération). */
    private Budget chargerOuLever(UUID budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RessourceIntrouvableException("Budget introuvable."));
    }
}
