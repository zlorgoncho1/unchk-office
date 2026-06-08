package sn.unchk.office.insertion.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.export.ExcelExporter;
import sn.unchk.office.common.export.PdfExporter;
import sn.unchk.office.insertion.dto.StatistiquesInsertion;
import sn.unchk.office.insertion.service.StatistiquesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API REST des statistiques d'insertion (auto-emploi vs emploi salarié).
 * <p>
 * Chemins sous {@code /api/insertion/statistiques}. Consultation réservée par le RBAC du
 * gateway aux rôles ayant accès à {@code /api/insertion/**} (admin, appui-insertion ; les
 * statistiques agrégées ne portent pas de données nominatives sensibles).
 * Export PDF / Excel via la librairie commune.
 */
@RestController
@RequestMapping("/api/insertion/statistiques")
public class StatistiquesController {

    private final StatistiquesService service;
    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;

    public StatistiquesController(StatistiquesService service,
                                  PdfExporter pdfExporter,
                                  ExcelExporter excelExporter) {
        this.service = service;
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
    }

    /** Statistiques d'insertion au format JSON. */
    @GetMapping
    public StatistiquesInsertion statistiques() {
        return service.calculer();
    }

    /** Export PDF des statistiques d'insertion. */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exporterPdf() {
        StatistiquesInsertion stats = service.calculer();
        byte[] contenu = pdfExporter.exporter(
                "Statistiques d'insertion — UNCHK Office",
                entetes(),
                lignes(stats));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistiques-insertion.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenu);
    }

    /** Export Excel (xlsx) des statistiques d'insertion. */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exporterExcel() {
        StatistiquesInsertion stats = service.calculer();
        byte[] contenu = excelExporter.exporter(
                "Insertion",
                entetes(),
                lignes(stats));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistiques-insertion.xlsx\"")
                .contentType(MediaType.valueOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenu);
    }

    /** En-têtes du tableau exporté (formation + une colonne par situation d'insertion). */
    private List<String> entetes() {
        List<String> entetes = new ArrayList<>();
        entetes.add("Formation");
        entetes.add("Total");
        // Une colonne par type d'insertion (auto_emploi, emploi_salarie, ...).
        for (sn.unchk.office.insertion.domain.InsertionKind kind
                : sn.unchk.office.insertion.domain.InsertionKind.values()) {
            entetes.add(kind.name());
        }
        return entetes;
    }

    /** Lignes du tableau : une par formation, plus une ligne « Total général ». */
    private List<List<String>> lignes(StatistiquesInsertion stats) {
        List<List<String>> lignes = new ArrayList<>();
        for (StatistiquesInsertion.StatistiqueFormation f : stats.parFormation()) {
            lignes.add(ligne(f.formationLabel(), f.total(), f.parType()));
        }
        // Ligne de synthèse globale.
        lignes.add(ligne("TOTAL GÉNÉRAL", stats.total(), stats.parType()));
        return lignes;
    }

    private List<String> ligne(String libelle, long total, Map<String, Long> parType) {
        List<String> ligne = new ArrayList<>();
        ligne.add(libelle);
        ligne.add(Long.toString(total));
        for (sn.unchk.office.insertion.domain.InsertionKind kind
                : sn.unchk.office.insertion.domain.InsertionKind.values()) {
            ligne.add(Long.toString(parType.getOrDefault(kind.name(), 0L)));
        }
        return ligne;
    }
}
