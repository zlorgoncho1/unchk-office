package sn.unchk.office.academic.emploidutemps;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA : stocke le jour de la semaine sous son libellé de base ({@code valeurDb})
 * et le relit en énumération.
 */
@Converter(autoApply = false)
public class JourSemaineConverter implements AttributeConverter<JourSemaine, String> {

    @Override
    public String convertToDatabaseColumn(JourSemaine valeur) {
        return valeur != null ? valeur.valeurDb() : null;
    }

    @Override
    public JourSemaine convertToEntityAttribute(String valeur) {
        return JourSemaine.depuis(valeur);
    }
}
