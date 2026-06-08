package sn.unchk.office.communication.domain;

/**
 * Type de réunion / d'événement (aligné sur l'énumération PostgreSQL {@code meeting_type}).
 */
public enum MeetingType {
    /** Réunion ordinaire. */
    reunion,
    /** Séminaire. */
    seminaire,
    /** Webinaire (à distance). */
    webinaire,
    /** Conseil d'Université. */
    conseil_universite,
    /** Tutorat. */
    tutorat,
    /** Préparation de cours. */
    preparation_cours,
    /** Évaluation. */
    evaluation,
    /** Rencontre (échange ponctuel, point informel). */
    rencontre
}
