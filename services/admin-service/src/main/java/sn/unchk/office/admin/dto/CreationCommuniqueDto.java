package sn.unchk.office.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.unchk.office.admin.domain.AdminDocKind;

import java.time.LocalDate;

/**
 * Données d'entrée pour créer un communiqué (note de service / circulaire).
 * <p>
 * L'{@code audience} est un préréglage de diffusion (tous, personnel, enseignants, etudiants,
 * administration) converti côté serveur en liste de rôles destinataires.
 *
 * @param kind      nature (note de service / circulaire)
 * @param title     titre / objet
 * @param body      corps (optionnel)
 * @param audience  audience cible (préréglage de rôles)
 * @param issueDate date d'émission (optionnelle ; aujourd'hui par défaut)
 * @param reference référence (optionnelle, unique si renseignée)
 */
public record CreationCommuniqueDto(
        @NotNull(message = "La nature est obligatoire.")
        AdminDocKind kind,

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 2000, message = "Le titre est trop long.")
        String title,

        @Size(max = 8000, message = "Le corps est trop long.")
        String body,

        @NotBlank(message = "L'audience est obligatoire.")
        String audience,

        LocalDate issueDate,

        @Size(max = 64, message = "La référence est trop longue.")
        String reference
) {
}
