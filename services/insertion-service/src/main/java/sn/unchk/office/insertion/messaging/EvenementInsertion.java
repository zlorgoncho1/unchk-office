package sn.unchk.office.insertion.messaging;

/**
 * Types d'événements métier publiés sur le topic {@code insertion.events}.
 * <p>
 * Le type est porté par le champ {@code eventType} de l'enveloppe {@link
 * sn.unchk.office.common.messaging.DomainEvent}. Les consommateurs (admin pour les stats,
 * communication pour le suivi, people) s'en servent pour router le traitement.
 */
public final class EvenementInsertion {

    private EvenementInsertion() {
        // Classe de constantes.
    }

    /** Un partenaire a été créé. */
    public static final String PARTENAIRE_CREE = "PartenaireCree";
    /** Un partenaire a été mis à jour. */
    public static final String PARTENAIRE_MODIFIE = "PartenaireModifie";
    /** Un partenaire a été supprimé (logique). */
    public static final String PARTENAIRE_SUPPRIME = "PartenaireSupprime";

    /** Un stage / bilan de stage a été créé. */
    public static final String STAGE_CREE = "StageCree";
    /** Un stage / bilan de stage a été mis à jour. */
    public static final String STAGE_MODIFIE = "StageModifie";
    /** Un stage a été clôturé / validé. */
    public static final String STAGE_VALIDE = "StageValide";

    /** Un contact de suivi a été enregistré. */
    public static final String CONTACT_ENREGISTRE = "ContactEnregistre";

    /** Une situation d'insertion a été déclarée. */
    public static final String INSERTION_DECLAREE = "InsertionDeclaree";
    /** Une situation d'insertion a été mise à jour. */
    public static final String INSERTION_MODIFIEE = "InsertionModifiee";
}
