package sn.unchk.office.academic.formation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA : stocke le financement sous son libellé de base ({@code valeurDb})
 * et le relit en énumération.
 */
@Converter(autoApply = false)
public class FinancementConverter implements AttributeConverter<Financement, String> {

    @Override
    public String convertToDatabaseColumn(Financement valeur) {
        return valeur != null ? valeur.valeurDb() : null;
    }

    @Override
    public Financement convertToEntityAttribute(String valeur) {
        return Financement.depuis(valeur);
    }
}
