package sn.unchk.office.admin.domain;

/**
 * Statut du cycle de vie d'un budget (aligné sur le type ENUM PostgreSQL {@code budget_status}).
 */
public enum BudgetStatus {
    /** Projet de budget en cours de saisie. */
    projet,
    /** Budget voté par l'instance compétente. */
    vote,
    /** Budget en cours d'exécution (réalisation des lignes). */
    en_execution,
    /** Exercice clôturé. */
    cloture
}
