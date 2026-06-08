package sn.unchk.office.insertion.domain;

/**
 * Type de partenaire (structure d'accueil).
 * Correspond au type énuméré PostgreSQL {@code partner_kind}.
 */
public enum PartnerKind {
    /** Entreprise privée. */
    entreprise,
    /** Administration publique. */
    administration,
    /** Organisation non gouvernementale. */
    ong,
    /** Institution (établissement public, agence...). */
    institution,
    /** Autre type de structure. */
    autre
}
