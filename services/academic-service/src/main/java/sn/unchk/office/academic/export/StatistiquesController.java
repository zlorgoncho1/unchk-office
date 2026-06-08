package sn.unchk.office.academic.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.academic.formation.FormationService;
import sn.unchk.office.common.export.ExcelExporter;
import sn.unchk.office.common.export.PdfExporter;

import java.util.List;

/**
 * Statistiques des formations (effectifs par genre) et exports PDF / Excel.
 * <p>
 * Réutilise les exporteurs de la librairie commune ({@link PdfExporter}, {@link ExcelExporter}).
 * Endpoints de collection : le RBAC de route au gateway suffit (pas d'objet ciblé par UUID).
 */
@RestController
@RequestMapping("/api/academic/statistiques")
public class StatistiquesController {

    /** En-têtes du tableau de statistiques (effectifs par genre). */
    private static final List<String> ENTETES =
            List.of("Code", "Intitulé", "Niveau", "Formés (H)", "Formés (F)", "Total");

    private final FormationService formationService;
    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;

    public StatistiquesController(FormationService formationService,
                                  PdfExporter pdfExporter,
                                  ExcelExporter excelExporter) {
        this.formationService = formationService;
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
    }

    /** Exporte les statistiques de formation (effectifs par genre) au format PDF. */
    @GetMapping(value = "/formations.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exporterPdf() {
        byte[] contenu = pdfExporter.exporter(
                "Statistiques des formations — effectifs par genre", ENTETES, lignes());
        return reponseFichier(contenu, MediaType.APPLICATION_PDF, "formations.pdf");
    }

    /** Exporte les statistiques de formation (effectifs par genre) au format Excel (xlsx). */
    @GetMapping("/formations.xlsx")
    public ResponseEntity<byte[]> exporterExcel() {
        byte[] contenu = excelExporter.exporter("Formations", ENTETES, lignes());
        MediaType xlsx = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return reponseFichier(contenu, xlsx, "formations.xlsx");
    }

    /** Construit les lignes du tableau à partir des formations actives. */
    private List<List<String>> lignes() {
        return formationService.lister().stream()
                .map(this::versLigne)
                .toList();
    }

    /** Transforme une formation en ligne de tableau (chaînes). */
    private List<String> versLigne(Formation f) {
        int total = f.getTrainedMale() + f.getTrainedFemale();
        return List.of(
                f.getCode() != null ? f.getCode() : "-",
                f.getLabel(),
                f.getLevel() != null ? f.getLevel().valeurDb() : "-",
                String.valueOf(f.getTrainedMale()),
                String.valueOf(f.getTrainedFemale()),
                String.valueOf(total));
    }

    /** Prépare la réponse HTTP de téléchargement (pièce jointe). */
    private ResponseEntity<byte[]> reponseFichier(byte[] contenu, MediaType type, String nomFichier) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .body(contenu);
    }
}
