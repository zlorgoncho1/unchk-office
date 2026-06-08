package sn.unchk.office.document.messaging;

/**
 * Noms des en-têtes Kafka portant l'enveloppe d'événement (cf. architecture.md §3.3).
 * <p>
 * L'enveloppe vit dans les headers (pas dans le payload) pour éviter la divergence ;
 * la valeur du message ne porte que l'état de l'agrégat.
 */
public final class EnteteEvenement {

    private EnteteEvenement() {
        // Classe de constantes.
    }

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_VERSION = "eventVersion";
    public static final String AGGREGATE_TYPE = "aggregateType";
    public static final String AGGREGATE_ID = "aggregateId";
    public static final String OCCURRED_AT = "occurredAt";
    public static final String TRACE_ID = "traceId";
    public static final String PRODUCER = "producer";

    /** Type d'agrégat publié par ce service. */
    public static final String AGGREGAT_DOCUMENT = "Document";

    /** Nom du service producteur (en-tête {@code producer}). */
    public static final String NOM_PRODUCTEUR = "document-service";
}
