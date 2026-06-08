package sn.unchk.office.admin.domain;

/**
 * Sens d'une ligne budgétaire (contrainte CHECK {@code direction IN ('depense','recette')}).
 */
public enum BudgetLineDirection {
    /** Poste de dépense. */
    depense,
    /** Poste de recette. */
    recette
}
