package sn.unchk.office.document.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mise à jour des métadonnées d'un document existant (titre, description, archivage,
 * visibilité par rôle). Le binaire MinIO n'est pas modifié par cette requête.
 *
 * @param title       nouveau titre (facultatif)
 * @param description nouvelle description (facultative)
 * @param archived    nouvel état d'archivage (facultatif)
 * @param visibility  nouvelle liste de rôles autorisés (remplace l'ancienne si fournie)
 */
public record MettreAJourDocumentRequete(

        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
        String title,

        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
        String description,

        Boolean archived,

        List<String> visibility
) {
}
