package sn.unchk.office.people.domain;

/**
 * Type de personnel / formateur (type enumere PostgreSQL {@code staff_kind}).
 * Couvre enseignants, associes, responsables de formation, tuteurs,
 * administratifs et personnel d'appui a l'insertion.
 */
public enum StaffKind {
    enseignant,
    enseignant_associe,
    responsable_formation,
    tuteur,
    administratif,
    appui_insertion
}
