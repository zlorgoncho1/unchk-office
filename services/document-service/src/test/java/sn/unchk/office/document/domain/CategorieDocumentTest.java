package sn.unchk.office.document.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de la conversion entre code « base » et énuméré de catégorie.
 */
class CategorieDocumentTest {

    @Test
    void leCodeBaseEstEnMinuscule() {
        // Le code persisté doit correspondre à l'énuméré PostgreSQL en minuscule.
        assertThat(CategorieDocument.NOTE_SERVICE.code()).isEqualTo("note_service");
        assertThat(CategorieDocument.CIRCULAIRE.code()).isEqualTo("circulaire");
        assertThat(CategorieDocument.COURRIER.code()).isEqualTo("courrier");
    }

    @Test
    void onRetrouveLEnumereDepuisSonCode() {
        // La conversion inverse doit être insensible à la casse.
        assertThat(CategorieDocument.depuisCode("note_service")).isEqualTo(CategorieDocument.NOTE_SERVICE);
        assertThat(CategorieDocument.depuisCode("CIRCULAIRE")).isEqualTo(CategorieDocument.CIRCULAIRE);
    }

    @Test
    void unCodeInconnuLeveUneErreur() {
        // Un code hors énuméré doit être rejeté explicitement.
        assertThatThrownBy(() -> CategorieDocument.depuisCode("inexistant"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
