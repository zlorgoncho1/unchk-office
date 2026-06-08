package sn.unchk.office.common.messaging;

/**
 * Noms des topics Kafka du domaine UNCHK Office.
 * <p>
 * Source de vérité unique des noms de topics, partagée par tous les services pour éviter
 * les fautes de frappe entre producteurs et consommateurs. Communication 100% Kafka :
 * chaque service publie ses événements et consomme ceux des autres pour ses read-models.
 */
public final class Topics {

    private Topics() {
        // Classe de constantes : pas d'instanciation.
    }

    /** identity-service : cycle de vie des comptes utilisateurs. */
    public static final String IDENTITY_USERS = "identity.users";

    /** people-service : référentiel canonique des étudiants. */
    public static final String PEOPLE_STUDENTS = "people.students";

    /** people-service : référentiel canonique du personnel / formateurs. */
    public static final String PEOPLE_STAFF = "people.staff";

    /** document-service : documents archivés (métadonnées ; binaire dans MinIO). */
    public static final String DOCUMENT_DOCUMENTS = "document.documents";

    /** communication-service : comptes rendus de réunions/séminaires. */
    public static final String COMMUNICATION_COMPTESRENDUS = "communication.comptesrendus";

    /** communication-service : réunions planifiées. */
    public static final String COMMUNICATION_REUNIONS = "communication.reunions";

    /** communication-service : notifications poussées (WebSocket vers le frontend). */
    public static final String NOTIFICATIONS = "notifications";

    /** academic-service : formations (détails, niveaux, financement...). */
    public static final String ACADEMIC_FORMATIONS = "academic.formations";

    /** insertion-service : événements d'insertion (stages, emplois, partenaires). */
    public static final String INSERTION_EVENTS = "insertion.events";

    /** admin-service : budget (projet de budget, budget réalisé). */
    public static final String ADMIN_BUDGET = "admin.budget";
}
