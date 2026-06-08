package sn.unchk.office.document.domain;

/**
 * Catégorie d'un document (reflète l'énuméré {@code document_category} de la base).
 * <p>
 * Le périmètre du service couvre notamment le courrier, les notes de service, les
 * notes administratives et les circulaires. Le nom de la constante est en MAJUSCULES,
 * mais la valeur persistée est le code « base » en minuscules (voir {@link #code()}).
 */
public enum CategorieDocument {

    LOGO("logo"),
    COMPTE_RENDU("compte_rendu"),
    COURRIER("courrier"),
    NOTE_SERVICE("note_service"),
    CIRCULAIRE("circulaire"),
    RAPPORT("rapport"),
    AUTRE("autre");

    private final String code;

    CategorieDocument(String code) {
        this.code = code;
    }

    /** Code persisté en base (minuscule, ex : {@code note_service}). */
    public String code() {
        return code;
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
