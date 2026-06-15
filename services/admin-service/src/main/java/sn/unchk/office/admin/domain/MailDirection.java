package sn.unchk.office.admin.domain;

/**
 * Sens d'un courrier (aligné sur le type ENUM PostgreSQL {@code mail_direction}).
 */
public enum MailDirection {
    /** Courrier arrivé (entrant). */
    arrive,
    /** Courrier départ (sortant). */
    depart
}
