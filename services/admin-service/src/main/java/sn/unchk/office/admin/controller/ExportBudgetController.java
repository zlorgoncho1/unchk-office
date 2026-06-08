package sn.unchk.office.admin.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.admin.service.ExportBudgetService;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.UUID;

/**
 * API REST d'export des états budgétaires (PDF / Excel) sous {@code /api/admin/budgets/{id}/export}.
 * <p>
 * L'accès à l'objet exporté est protégé par l'ABAC anti-IDOR ({@link VerifieAccesObjet}) :
 * on ne peut exporter que les budgets que l'on est autorisé à lire. Les binaires sont produits
 * en mémoire par les utilitaires de libs/common (PdfExporter / ExcelExporter).
 */
@RestController
@RequestMapping("/api/admin/budgets/{id}/export")
public class ExportBudgetController {

    /** Type MIME des classeurs Excel xlsx. */
    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportBudgetService exportService;

    public ExportBudgetController(ExportBudgetService exportService) {
        this.exportService = exportService;
    }

    /** Exporte l'état budgétaire au format PDF. */
    @GetMapping("/pdf")
    @VerifieAccesObjet(type = "budget", action = "read", idParam = "id")
    public ResponseEntity<byte[]> exporterPdf(@PathVariable UUID id) {
        byte[] contenu = exportService.exporterPdf(id);
        return reponseFichier(contenu, MediaType.APPLICATION_PDF_VALUE, "budget-" + id + ".pdf");
    }

    /** Exporte l'état budgétaire au format Excel (xlsx). */
    @GetMapping("/excel")
    @VerifieAccesObjet(type = "budget", action = "read", idParam = "id")
    public ResponseEntity<byte[]> exporterExcel(@PathVariable UUID id) {
        byte[] contenu = exportService.exporterExcel(id);
        return reponseFichier(contenu, TYPE_XLSX, "budget-" + id + ".xlsx");
    }

    /** Construit la réponse binaire avec les en-têtes de téléchargement adéquats. */
    private ResponseEntity<byte[]> reponseFichier(byte[] contenu, String typeMime, String nomFichier) {
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.parseMediaType(typeMime));
        entetes.setContentDisposition(ContentDisposition.attachment().filename(nomFichier).build());
        // Données sensibles : pas de mise en cache.
        entetes.setCacheControl("no-store");
        return ResponseEntity.ok().headers(entetes).body(contenu);
    }
}
