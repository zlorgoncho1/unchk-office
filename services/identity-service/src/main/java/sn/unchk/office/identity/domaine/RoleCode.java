package sn.unchk.office.identity.domaine;

/**
 * Les cinq rôles applicatifs de la plateforme UNCHK Office.
 * <p>
 * Correspond au type énuméré PostgreSQL {@code role_code}. Les libellés exposés (et stockés
 * en base / publiés sur Kafka) reprennent exactement les valeurs attendues par OPA :
 * {@code admin}, {@code administratif}, {@code enseignant}, {@code appui-insertion}, {@code etudiant}.
 */
public enum RoleCode {

    ADMIN("admin"),
    ADMINISTRATIF("administratif"),
    ENSEIGNANT("enseignant"),
    APPUI_INSERTION("appui-insertion"),
    ETUDIANT("etudiant");

    /** Libellé canonique (tel qu'attendu par OPA et stocké en base). */
    private final String libelle;

    RoleCode(String libelle) {
        this.libelle = libelle;
    }

    /** Renvoie le libellé canonique du rôle (ex : {@code appui-insertion}). */
    public String libelle() {
        return libelle;
    }

    /**
     * Convertit un libellé textuel en rôle, en tolérant la casse et les espaces.
     *
     * @param valeur libellé du rôle (ex : "etudiant")
     * @return le rôle correspondant
     * @throws IllegalArgumentException si le libellé ne correspond à aucun rôle connu
     */
    public static RoleCode depuisLibelle(String valeur) {
        if (valeur != null) {
            String normalise = valeur.trim().toLowerCase();
            for (RoleCode role : values()) {
                if (role.libelle.equals(normalise)) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("Rôle inconnu : " + valeur);
    }
}
