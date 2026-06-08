package sn.unchk.office.people.domain;

/**
 * Statut d'un etudiant (contrainte CHECK de la colonne {@code students.status}).
 */
public enum StudentStatus {
    inscrit,
    diplome,
    abandon,
    suspendu
}
