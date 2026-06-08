package sn.unchk.office.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Métadonnées soumises lors du dépôt d'un document (envoyées en champ « form-data »
 * à côté du fichier binaire). Le binaire lui-même est porté par le {@code MultipartFile}.
 *
 * @param title       titre du document (obligatoire)
 * @param category    catégorie métier (courrier, note_service, circulaire, ...)
 * @param description description libre (facultative)
 * @param visibility  rôles autorisés à voir le document (visibility[] OPA)
 * @param sourceService service métier d'origine (facultatif, ex : admin-service)
 * @param sourceRef   identifiant métier d'origine (facultatif)
 */
public record CreerDocumentRequete(

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
        String title,

        @NotBlank(message = "La catégorie est obligatoire.")
        @Pattern(
                regexp = "logo|compte_rendu|courrier|note_service|circulaire|rapport|autre",
                message = "Catégorie invalide.")
        String category,

        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
        String description,

        List<@NotBlank String> visibility,

        @Size(max = 100, message = "Le service source ne doit pas dépasser 100 caractères.")
        String sourceService,

        UUID sourceRef
) {
}
