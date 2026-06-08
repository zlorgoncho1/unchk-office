package sn.unchk.office.common.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Utilitaire d'export de rapports au format PDF via OpenPDF (fork libre d'iText).
 * <p>
 * Produit un document A4 avec un titre et un tableau (en-têtes + lignes). Les couleurs
 * suivent la charte UNCHK (bleu primaire pour l'en-tête du tableau). Le résultat est
 * renvoyé en octets, prêt à être streamé en réponse HTTP.
 */
public class PdfExporter {

    /** Bleu primaire de la charte UNCHK (#1C75BC) pour les en-têtes de tableau. */
    private static final Color BLEU_UNCHK = new Color(0x1C, 0x75, 0xBC);

    /**
     * Génère un PDF contenant un titre puis un tableau de données.
     *
     * @param titre   titre du rapport (en tête de page)
     * @param entetes libellés des colonnes
     * @param lignes  données : une liste de lignes, chaque ligne étant une liste de cellules
     * @return contenu binaire du fichier PDF
     */
    public byte[] exporter(String titre, List<String> entetes, List<List<String>> lignes) {
        Document document = new Document();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, sortie);
            document.open();

            // Titre du rapport.
            Font policeTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0x16, 0x31, 0x4A));
            Paragraph paragrapheTitre = new Paragraph(titre, policeTitre);
            paragrapheTitre.setSpacingAfter(12f);
            document.add(paragrapheTitre);

            // Tableau : une colonne par en-tête.
            PdfPTable tableau = new PdfPTable(entetes.size());
            tableau.setWidthPercentage(100f);

            // Ligne d'en-tête : texte blanc sur fond bleu.
            Font policeEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            for (String entete : entetes) {
                PdfPCell cellule = new PdfPCell(new Phrase(entete, policeEntete));
                cellule.setBackgroundColor(BLEU_UNCHK);
                cellule.setPadding(6f);
                cellule.setHorizontalAlignment(Element.ALIGN_LEFT);
                tableau.addCell(cellule);
            }

            // Lignes de données.
            Font policeCellule = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (List<String> ligne : lignes) {
                for (String valeur : ligne) {
                    PdfPCell cellule = new PdfPCell(new Phrase(valeur, policeCellule));
                    cellule.setPadding(5f);
                    tableau.addCell(cellule);
                }
            }

            document.add(tableau);
            document.close();
            return sortie.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            // La génération en mémoire échoue rarement ; on remonte une erreur non vérifiée.
            throw new IllegalStateException("Échec de la génération du PDF", ex);
        }
    }
}
