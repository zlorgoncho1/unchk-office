package sn.unchk.office.common.export;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des utilitaires d'export : expose les exporteurs Excel et PDF comme beans.
 */
@Configuration
public class ConfigurationExport {

    @Bean
    public ExcelExporter excelExporter() {
        return new ExcelExporter();
    }

    @Bean
    public PdfExporter pdfExporter() {
        return new PdfExporter();
    }
}
