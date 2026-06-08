package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.domain.BudgetLine;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux lignes budgétaires.
 */
public interface BudgetLineRepository extends JpaRepository<BudgetLine, UUID> {

    /** Lignes d'un budget, ordonnées par poste pour un affichage stable. */
    List<BudgetLine> findByBudgetIdOrderByCategoryAsc(UUID budgetId);

    /** Supprime toutes les lignes d'un budget (utilisé lors de la suppression du budget). */
    void deleteByBudgetId(UUID budgetId);
}
