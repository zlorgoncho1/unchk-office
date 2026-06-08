package sn.unchk.office.common.export;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de l'exporteur Excel {@link ExcelExporter}.
 * Génère un classeur puis le relit avec POI pour vérifier le contenu produit.
 */
class ExcelExporterTest {

    private final ExcelExporter exporter = new ExcelExporter();

    @Test
    void genere_un_classeur_xlsx_lisible_avec_entetes_et_donnees() throws Exception {
        // Étant donné un en-tête et deux lignes de données
        List<String> entetes = List.of("INE", "Nom", "Formation");
        List<List<String>> lignes = List.of(
                List.of("INE-001", "Diop", "Génie Logiciel"),
                List.of("INE-002", "Ndiaye", "Réseaux"));

        // Quand on exporte en Excel
        byte[] contenu = exporter.exporter("Étudiants", entetes, lignes);

        // Alors le binaire produit est non vide
        assertThat(contenu).isNotEmpty();

        // Et il est relisible : on vérifie l'en-tête et la première ligne de données
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheetAt(0);
            assertThat(feuille.getSheetName()).isEqualTo("Étudiants");

            // Ligne d'en-tête
            assertThat(feuille.getRow(0).getCell(0).getStringCellValue()).isEqualTo("INE");
            assertThat(feuille.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Formation");

            // Première ligne de données
            assertThat(feuille.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Diop");
            assertThat(feuille.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Réseaux");
        }
    }
}
