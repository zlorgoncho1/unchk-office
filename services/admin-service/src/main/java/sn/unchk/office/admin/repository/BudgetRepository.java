package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.domain.Budget;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux budgets. Toutes les requêtes sont paramétrées par Spring Data (anti-injection SQL).
 */
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /** Vérifie l'unicité (exercice, libellé) avant création. */
    boolean existsByFiscalYearAndLabel(Short fiscalYear, String label);

    /** Liste les budgets d'un exercice donné. */
    List<Budget> findByFiscalYearOrderByLabelAsc(Short fiscalYear);
}
