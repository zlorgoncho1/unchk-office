package sn.unchk.office.academic.formation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type / nature d'une formation. La valeur SQL/JSON ({@link #valeurDb()}) correspond
 * exactement aux libellés autorisés en base (colonne {@code kind} contrainte par CHECK).
 * <p>
 * Note : {@code continue} est un mot réservé Java ; le constant est donc nommé {@code CONTINUE}
 * et porte sa propre valeur de base ({@code "continue"}).
 */
public enum TypeFormation {
    INITIALE("initiale"),
    CONTINUE("continue"),
    PROFESSIONNELLE("professionnelle"),
    DIPLOMANTE("diplomante"),
    QUALIFIANTE("qualifiante");

    private final String valeurDb;

    TypeFormation(String valeurDb) {
        this.valeurDb = valeurDb;
    }

    /** Libellé stocké en base et exposé en JSON. */
    @JsonValue
    public String valeurDb() {
        return valeurDb;
    }

    /** Résout l'énumération depuis le libellé (insensible à la casse), pour la désérialisation. */
    @JsonCreator
    public static TypeFormation depuis(String valeur) {
        if (valeur == null) {
            return null;
        }
        for (TypeFormation t : values()) {
            if (t.valeurDb.equalsIgnoreCase(valeur) || t.name().equalsIgnoreCase(valeur)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Type de formation inconnu : " + valeur);
    }
}
