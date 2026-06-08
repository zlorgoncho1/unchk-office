package sn.unchk.office.academic.emploidutemps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Jour de la semaine d'un créneau récurrent. La valeur SQL/JSON ({@link #valeurDb()}) correspond
 * exactement aux libellés autorisés en base (colonne {@code day_of_week} contrainte par CHECK).
 */
public enum JourSemaine {
    LUNDI("lundi"),
    MARDI("mardi"),
    MERCREDI("mercredi"),
    JEUDI("jeudi"),
    VENDREDI("vendredi"),
    SAMEDI("samedi"),
    DIMANCHE("dimanche");

    private final String valeurDb;

    JourSemaine(String valeurDb) {
        this.valeurDb = valeurDb;
    }

    /** Libellé stocké en base et exposé en JSON. */
    @JsonValue
    public String valeurDb() {
        return valeurDb;
    }

    /** Résout l'énumération depuis le libellé (insensible à la casse), pour la désérialisation. */
    @JsonCreator
    public static JourSemaine depuis(String valeur) {
        if (valeur == null) {
            return null;
        }
        for (JourSemaine j : values()) {
            if (j.valeurDb.equalsIgnoreCase(valeur) || j.name().equalsIgnoreCase(valeur)) {
                return j;
            }
        }
        throw new IllegalArgumentException("Jour de la semaine inconnu : " + valeur);
    }
}
