package sn.unchk.office.admin.domain;

/**
 * Statut de traitement d'un courrier (aligné sur le type ENUM PostgreSQL {@code mail_status}).
 */
public enum MailStatus {
    /** Reçu / enregistré. */
    recu,
    /** En cours de traitement. */
    en_traitement,
    /** Traité. */
    traite,
    /** Archivé. */
    archive,
    /** Clos. */
    clos
}
