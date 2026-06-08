package sn.unchk.office.academic.formation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA : stocke le type de formation sous son libellé de base ({@code valeurDb})
 * et le relit en énumération. Indispensable car {@code continue} (mot réservé Java) ne peut
 * être le nom du constant : on persiste « continue » via {@code valeurDb()}.
 */
@Converter(autoApply = false)
public class TypeFormationConverter implements AttributeConverter<TypeFormation, String> {

    @Override
    public String convertToDatabaseColumn(TypeFormation valeur) {
        return valeur != null ? valeur.valeurDb() : null;
    }

    @Override
    public TypeFormation convertToEntityAttribute(String valeur) {
        return TypeFormation.depuis(valeur);
    }
}
