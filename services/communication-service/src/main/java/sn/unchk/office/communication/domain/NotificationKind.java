package sn.unchk.office.communication.domain;

/**
 * Catégorie d'une notification (aligné sur l'énumération PostgreSQL {@code notification_kind}).
 */
public enum NotificationKind {
    /** Publication d'un compte rendu. */
    compte_rendu,
    /** Circulaire (provient du module documentaire). */
    circulaire,
    /** Note de service. */
    note_service,
    /** Convocation / information de réunion. */
    reunion,
    /** Courrier. */
    courrier,
    /** Notification système. */
    systeme
}
