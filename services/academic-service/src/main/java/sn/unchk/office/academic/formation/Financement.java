package sn.unchk.office.academic.formation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Source de financement d'une formation. La valeur SQL/JSON ({@link #valeurDb()}) correspond
 * exactement aux libellés autorisés en base (colonne {@code funding} contrainte par CHECK).
 */
public enum Financement {
    ETAT("etat"),
    PARTENAIRE("partenaire"),
    AUTOFINANCEMENT("autofinancement"),
    PROJET("projet"),
    MIXTE("mixte");

    private final String valeurDb;

    Financement(String valeurDb) {
        this.valeurDb = valeurDb;
    }

    /** Libellé stocké en base et exposé en JSON. */
    @JsonValue
    public String valeurDb() {
        return valeurDb;
    }

    /** Résout l'énumération depuis le libellé (insensible à la casse), pour la désérialisation. */
    @JsonCreator
    public static Financement depuis(String valeur) {
        if (valeur == null) {
            return null;
        }
        for (Financement f : values()) {
            if (f.valeurDb.equalsIgnoreCase(valeur) || f.name().equalsIgnoreCase(valeur)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Financement inconnu : " + valeur);
    }
}
