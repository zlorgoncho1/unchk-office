package sn.unchk.office.document.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mise à jour des métadonnées d'un document existant (titre, catégorie, description,
 * archivage, visibilité par rôle). Le binaire MinIO n'est pas modifié par cette requête.
 *
 * @param title       nouveau titre (facultatif)
 * @param category    nouvelle catégorie métier (facultative, codes de {@code document_category})
 * @param description nouvelle description (facultative)
 * @param archived    nouvel état d'archivage (facultatif)
 * @param visibility  nouvelle liste de rôles autorisés (remplace l'ancienne si fournie)
 */
public record MettreAJourDocumentRequete(

        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
        String title,

        @Pattern(
                regexp = "logo|compte_rendu|courrier|courrier_arrive|courrier_depart"
                        + "|note_service|note_service_interne|note_service_externe"
                        + "|note_administrative|circulaire|rapport|autre",
                message = "Catégorie invalide.")
        String category,

        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
        String description,

        Boolean archived,

        List<String> visibility
) {
}
