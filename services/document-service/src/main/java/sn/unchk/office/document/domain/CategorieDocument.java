package sn.unchk.office.document.domain;

/**
 * Catégorie d'un document (reflète l'énuméré {@code document_category} de la base).
 * <p>
 * Le périmètre du service couvre notamment le courrier (arrivé / départ), les notes de
 * service (interne / externe), les notes administratives et les circulaires, conformément
 * à l'énoncé. Le nom de la constante est en MAJUSCULES, mais la valeur persistée est le
 * code « base » en minuscules (voir {@link #code()}).
 * <p>
 * Les anciennes catégories génériques ({@code courrier}, {@code note_service}) sont
 * conservées pour assurer la compatibilité avec les documents déjà déposés.
 */
public enum CategorieDocument {

    LOGO("logo", "Logo"),
    COMPTE_RENDU("compte_rendu", "Compte rendu"),
    // Catégorie générique « courrier » conservée pour compatibilité ascendante.
    COURRIER("courrier", "Courrier"),
    COURRIER_ARRIVE("courrier_arrive", "Courrier arrivé"),
    COURRIER_DEPART("courrier_depart", "Courrier départ"),
    // Catégorie générique « note_service » conservée pour compatibilité ascendante.
    NOTE_SERVICE("note_service", "Note de service"),
    NOTE_SERVICE_INTERNE("note_service_interne", "Note de service interne"),
    NOTE_SERVICE_EXTERNE("note_service_externe", "Note de service externe"),
    NOTE_ADMINISTRATIVE("note_administrative", "Note administrative"),
    CIRCULAIRE("circulaire", "Circulaire"),
    RAPPORT("rapport", "Rapport"),
    AUTRE("autre", "Autre");

    private final String code;
    private final String libelle;

    CategorieDocument(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /** Code persisté en base (minuscule, ex : {@code note_service_interne}). */
    public String code() {
        return code;
    }

    /** Libellé lisible (français) de la catégorie, destiné à l'IHM. */
    public String libelle() {
        return libelle;
    }

    /**
     * Convertit un code « base » en énuméré.
     *
     * @param code code de catégorie (minuscule)
     * @return la catégorie correspondante
     * @throws IllegalArgumentException si le code est inconnu
     */
    public static CategorieDocument depuisCode(String code) {
        for (CategorieDocument categorie : values()) {
            if (categorie.code.equalsIgnoreCase(code)) {
                return categorie;
            }
        }
        throw new IllegalArgumentException("Catégorie de document inconnue : " + code);
    }
}
