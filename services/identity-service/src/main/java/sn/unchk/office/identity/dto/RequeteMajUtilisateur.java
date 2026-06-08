package sn.unchk.office.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corps de mise à jour d'un compte ({@code PUT /api/identity/users/{id}}), réservé à l'admin.
 * <p>
 * Tous les champs sont optionnels (mise à jour partielle) ; seuls les champs renseignés
 * sont appliqués. Le courriel et l'identifiant ne sont pas modifiables ici (anti sur-affectation).
 *
 * @param fullName nouveau nom complet (optionnel)
 * @param active   nouvel état d'activation (optionnel)
 * @param locked   nouvel état de verrouillage (optionnel ; false pour déverrouiller)
 * @param roles    nouvelle liste de rôles (optionnel ; remplace l'existante si fournie)
 */
public record RequeteMajUtilisateur(

        @Size(max = 200, message = "Le nom complet est trop long.")
        String fullName,

        Boolean active,

        Boolean locked,

        List<@Pattern(regexp = "admin|administratif|enseignant|appui-insertion|etudiant",
                message = "Rôle invalide.") String> roles
) {
}
