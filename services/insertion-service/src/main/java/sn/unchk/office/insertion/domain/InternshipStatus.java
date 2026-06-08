package sn.unchk.office.insertion.domain;

/**
 * Statut d'un stage. Correspond au type énuméré PostgreSQL {@code internship_status}.
 */
public enum InternshipStatus {
    /** Stage prévu (non démarré). */
    prevu,
    /** Stage en cours. */
    en_cours,
    /** Stage terminé. */
    termine,
    /** Stage rompu avant son terme. */
    rompu,
    /** Stage validé (bilan clôturé). */
    valide
}
