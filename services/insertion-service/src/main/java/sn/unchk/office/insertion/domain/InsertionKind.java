package sn.unchk.office.insertion.domain;

/**
 * Situation d'insertion d'un diplômé (pour les statistiques auto-emploi vs salarié).
 * Correspond au type énuméré PostgreSQL {@code insertion_kind}.
 */
public enum InsertionKind {
    /** Emploi salarié. */
    emploi_salarie,
    /** Auto-emploi / entrepreneuriat. */
    auto_emploi,
    /** En recherche d'emploi. */
    recherche_emploi,
    /** Poursuite d'études. */
    poursuite_etudes,
    /** Sans activité déclarée. */
    sans_activite
}
