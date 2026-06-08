package sn.unchk.office.communication.domain;

/**
 * Statut d'une réunion (aligné sur l'énumération PostgreSQL {@code meeting_status}).
 */
public enum MeetingStatus {
    /** Planifiée (état initial). */
    planifiee,
    /** En cours. */
    en_cours,
    /** Terminée. */
    terminee,
    /** Annulée. */
    annulee
}
