package sn.unchk.office.academic.formation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA : stocke le niveau sous son libellé de base ({@code valeurDb})
 * et le relit en énumération. Garantit l'alignement avec la contrainte CHECK SQL.
 */
@Converter(autoApply = false)
public class NiveauFormationConverter implements AttributeConverter<NiveauFormation, String> {

    @Override
    public String convertToDatabaseColumn(NiveauFormation valeur) {
        return valeur != null ? valeur.valeurDb() : null;
    }

    @Override
    public NiveauFormation convertToEntityAttribute(String valeur) {
        return NiveauFormation.depuis(valeur);
    }
}
