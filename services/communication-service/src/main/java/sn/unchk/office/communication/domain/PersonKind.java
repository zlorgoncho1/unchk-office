package sn.unchk.office.communication.domain;

/**
 * Nature d'un participant à une réunion (contrainte CHECK de {@code reunion_participants}).
 */
public enum PersonKind {
    /** Membre du personnel (people.staff). */
    staff,
    /** Étudiant (people.students). */
    student
}
