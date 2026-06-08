package sn.unchk.office.document.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA entre {@link CategorieDocument} et son code « base » en minuscule.
 * <p>
 * La colonne {@code category} stocke le code de l'énuméré PostgreSQL ({@code note_service},
 * {@code circulaire}, ...). On évite ainsi d'exposer le nom Java en majuscules en base.
 */
@Converter(autoApply = false)
public class ConvertisseurCategorie implements AttributeConverter<CategorieDocument, String> {

    @Override
    public String convertToDatabaseColumn(CategorieDocument categorie) {
        return categorie == null ? null : categorie.code();
    }

    @Override
    public CategorieDocument convertToEntityAttribute(String code) {
        return code == null ? null : CategorieDocument.depuisCode(code);
    }
}
