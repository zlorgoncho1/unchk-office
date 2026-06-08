package sn.unchk.office.common.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Utilitaire d'export de rapports au format Excel (xlsx) via Apache POI.
 * <p>
 * Conçu pour générer des tableaux simples (en-têtes + lignes) à partir de données déjà
 * préparées par les services (statistiques d'insertion, listes d'étudiants, budget...).
 * Le résultat est renvoyé sous forme de tableau d'octets, prêt à être streamé en réponse HTTP.
 */
public class ExcelExporter {

    /**
     * Génère un classeur xlsx à une feuille comportant une ligne d'en-tête et des lignes de données.
     *
     * @param titreFeuille nom de la feuille
     * @param entetes      libellés des colonnes (première ligne, en gras)
     * @param lignes       données : une liste de lignes, chaque ligne étant une liste de cellules
     * @return contenu binaire du fichier xlsx
     */
    public byte[] exporter(String titreFeuille, List<String> entetes, List<List<String>> lignes) {
        // try-with-resources : le classeur et le flux sont fermés automatiquement.
        try (Workbook classeur = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            Sheet feuille = classeur.createSheet(titreFeuille);

            // Ligne d'en-tête en gras.
            CellStyle styleEntete = styleGras(classeur);
            Row ligneEntete = feuille.createRow(0);
            for (int c = 0; c < entetes.size(); c++) {
                Cell cellule = ligneEntete.createCell(c);
                cellule.setCellValue(entetes.get(c));
                cellule.setCellStyle(styleEntete);
            }

            // Lignes de données, sous l'en-tête.
            int numeroLigne = 1;
            for (List<String> ligne : lignes) {
                Row r = feuille.createRow(numeroLigne++);
                for (int c = 0; c < ligne.size(); c++) {
                    r.createCell(c).setCellValue(ligne.get(c));
                }
            }

            // Ajuste la largeur des colonnes au contenu pour une meilleure lisibilité.
            for (int c = 0; c < entetes.size(); c++) {
                feuille.autoSizeColumn(c);
            }

            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            // L'écriture en mémoire ne devrait pas échouer ; on remonte une erreur non vérifiée.
            throw new UncheckedIOException("Échec de la génération du fichier Excel", ex);
        }
    }

    /** Construit le style « gras » réutilisé pour la ligne d'en-tête. */
    private CellStyle styleGras(Workbook classeur) {
        Font police = classeur.createFont();
        police.setBold(true);
        CellStyle style = classeur.createCellStyle();
        style.setFont(police);
        return style;
    }
}
