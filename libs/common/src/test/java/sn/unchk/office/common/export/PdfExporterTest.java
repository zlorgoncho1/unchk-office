package sn.unchk.office.common.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de l'exporteur PDF {@link PdfExporter}.
 * Vérifie qu'un PDF binaire valide est produit (en-tête de signature %PDF).
 */
class PdfExporterTest {

    private final PdfExporter exporter = new PdfExporter();

    @Test
    void genere_un_pdf_valide_avec_titre_et_tableau() {
        // Étant donné un titre, un en-tête et des données
        List<String> entetes = List.of("Indicateur", "Valeur");
        List<List<String>> lignes = List.of(
                List.of("Auto-emploi", "42"),
                List.of("Emploi salarié", "58"));

        // Quand on exporte en PDF
        byte[] contenu = exporter.exporter("Statistiques d'insertion", entetes, lignes);

        // Alors le binaire produit n'est pas vide
        assertThat(contenu).isNotEmpty();

        // Et il commence par la signature d'un fichier PDF ("%PDF")
        String entete = new String(contenu, 0, 4, StandardCharsets.US_ASCII);
        assertThat(entete).isEqualTo("%PDF");
    }
}
