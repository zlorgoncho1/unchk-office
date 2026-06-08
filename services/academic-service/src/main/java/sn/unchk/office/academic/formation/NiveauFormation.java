package sn.unchk.office.academic.formation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Niveau d'une formation. La valeur SQL/JSON ({@link #valeurDb()}) correspond exactement
 * aux libellés autorisés en base (colonne {@code level} contrainte par CHECK).
 */
public enum NiveauFormation {
    CERTIFICAT("certificat"),
    LICENCE("licence"),
    MASTER("master"),
    DOCTORAT("doctorat"),
    FORMATION_CONTINUE("formation_continue");

    private final String valeurDb;

    NiveauFormation(String valeurDb) {
        this.valeurDb = valeurDb;
    }

    /** Libellé stocké en base et exposé en JSON. */
    @JsonValue
    public String valeurDb() {
        return valeurDb;
    }

    /** Résout l'énumération depuis le libellé (insensible à la casse), pour la désérialisation. */
    @JsonCreator
    public static NiveauFormation depuis(String valeur) {
        if (valeur == null) {
            return null;
        }
        for (NiveauFormation n : values()) {
            if (n.valeurDb.equalsIgnoreCase(valeur) || n.name().equalsIgnoreCase(valeur)) {
                return n;
            }
        }
        throw new IllegalArgumentException("Niveau de formation inconnu : " + valeur);
    }
}
