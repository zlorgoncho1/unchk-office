package sn.unchk.office.admin.domain;

/**
 * Nature d'un communiqué administratif (aligné sur le type ENUM PostgreSQL {@code admin_doc_kind}).
 */
public enum AdminDocKind {
    /** Note de service (interne / externe). */
    note_service,
    /** Circulaire du niveau central. */
    circulaire
}
